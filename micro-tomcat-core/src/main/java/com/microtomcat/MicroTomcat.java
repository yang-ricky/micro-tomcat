package com.microtomcat;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.microtomcat.server.ServerConfig;
import com.microtomcat.server.AbstractHttpServer;
import com.microtomcat.server.HttpServerFactory;
import com.microtomcat.loader.ClassLoaderManager;
import com.microtomcat.lifecycle.LifecycleException;
import com.microtomcat.context.Context;
import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import com.microtomcat.session.SessionManager;
import com.microtomcat.context.SimpleServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.Servlet;

public class MicroTomcat extends AbstractHttpServer {
    private static final int DEFAULT_PORT = 8080;
    private final int port;
    private static final String WEB_ROOT = "webroot";
    private final ExecutorService executorService;
    private Context context;
    private SessionManager sessionManager;
    private volatile ServerSocket serverSocket;

    public MicroTomcat(int port) throws IOException {
        super(new ServerConfig(port, false, 10, WEB_ROOT));
        this.port = port;
        this.executorService = Executors.newFixedThreadPool(10);
        this.sessionManager = new SessionManager(new SimpleServletContext(""));
    }

    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    protected void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME);
        System.out.printf("[HttpServer %s] %s%n", timestamp, message);
    }

    @Override
    public void start() throws LifecycleException {
        try {
            ClassLoaderManager.init();
            
            serverSocket = new ServerSocket(port);
            log("Server started on port: " + port);
            
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket socket = serverSocket.accept();
                    log("New connection accepted from: " + socket.getInetAddress());
                    executorService.execute(() -> {
                        try {
                            handleRequest(socket);
                        } catch (Exception e) {
                            log("Error processing request: " + e.getMessage());
                        } finally {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                log("Error closing socket: " + e.getMessage());
                            }
                        }
                    });
                } catch (IOException e) {
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    throw e;
                }
            }
        } catch (Exception e) {
            throw new LifecycleException("Failed to start server", e);
        }
    }

    protected void handleRequest(Socket socket) throws IOException {
        try (InputStream input = socket.getInputStream();
             OutputStream output = socket.getOutputStream()) {
            
            Request request = new Request(input, sessionManager);
            Response response = new Response(output);
            
            request.parse();
            
            log("Handling request: " + request.getRequestURI());
            
            Context context = this.context;
            log("Using context: " + (context != null ? context.getName() : "null"));
            
            if (context != null) {
                try {
                    request.setContext(context);
                    context.invoke(request, response);
                    if (!response.isCommitted()) {
                        response.setStatus(HttpServletResponse.SC_OK);
                    }
                } catch (Exception e) {
                    log("Error processing request: " + e.getMessage());
                    response.setContentType("text/plain");
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    String errorMsg = "500 Internal Server Error: " + e.getMessage() + "\n";
                    output.write(errorMsg.getBytes());
                }
            } else {
                response.setContentType("text/plain");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                String errorMsg = "404 Not Found: " + request.getRequestURI() + "\n";
                output.write(errorMsg.getBytes());
            }
            response.flushBuffer();
        }
    }

    private String getContentType(String uri) {
        if (uri.endsWith(".html")) {
            return "text/html";
        } else if (uri.endsWith(".txt")) {
            return "text/plain";
        } else if (uri.endsWith(".css")) {
            return "text/css";
        } else if (uri.endsWith(".js")) {
            return "application/javascript";
        }
        return "application/octet-stream";
    }

    @Override
    protected void initInternal() throws LifecycleException {
        super.initInternal();
    }
    
    @Override
    protected void destroyInternal() throws LifecycleException {
        super.destroyInternal();
    }

    public static void main(String[] args) {
        // 解析命令行参数
        int port = DEFAULT_PORT; // 默认端口
        
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--port=")) {
                try {
                    port = Integer.parseInt(args[i].substring("--port=".length()));
                } catch (NumberFormatException e) {
                    System.err.println("Invalid port number: " + args[i]);
                    System.exit(1);
                }
            }
        }

        try {
            // 创建并配置 MicroTomcat
            MicroTomcat tomcat = new MicroTomcat(port);
            
            // 创建 Context
            String contextPath = "";
            String docBase = "webroot";
            Context context = tomcat.addContext(contextPath, docBase);
            
            // 启动服务器
            tomcat.init();
            tomcat.start();
            
            // 等待服务器停止
            Thread.currentThread().join();
            
        } catch (Exception e) {
            System.err.println("Server startup failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void stop() throws LifecycleException {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log("Error while stopping server: " + e.getMessage());
        }
        // 保持原有的线程池关闭逻辑
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
                throw new LifecycleException("Error while stopping server", e);
            }
        }
        log("Server stopped");
    }

    public Context addContext(String contextPath, String docBase) throws IOException {
        if (contextPath == null) {
            throw new IllegalArgumentException("contextPath cannot be null");
        }
        if (docBase == null) {
            throw new IllegalArgumentException("docBase cannot be null");
        }
        Context newContext = new Context(contextPath, docBase);
        this.context = newContext;
        return newContext;
    }

    public void addServlet(Context context, String servletName, Servlet servlet) {
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        if (servletName == null) {
            throw new IllegalArgumentException("servletName cannot be null");
        }
        if (servlet == null) {
            throw new IllegalArgumentException("servlet cannot be null");
        }
        context.addServlet(servletName, servlet);
    }

    public void addServletMapping(Context context, String urlPattern, String servletName) {
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        if (urlPattern == null) {
            throw new IllegalArgumentException("urlPattern cannot be null");
        }
        if (servletName == null) {
            throw new IllegalArgumentException("servletName cannot be null");
        }
        context.addServletMapping(urlPattern, servletName);
    }

    public void addServletMappingDecoded(Context context, String urlPattern, String servletName) {
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        if (urlPattern == null) {
            throw new IllegalArgumentException("urlPattern cannot be null");
        }
        if (servletName == null) {
            throw new IllegalArgumentException("servletName cannot be null");
        }
        context.addServletMapping(urlPattern, servletName);
    }

    public void addServlet(Context context, String servletName, Servlet servlet, String urlPattern) {
        addServlet(context, servletName, servlet);
        addServletMapping(context, urlPattern, servletName);
    }
}
