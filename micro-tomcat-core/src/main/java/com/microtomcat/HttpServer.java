package com.microtomcat;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import javax.servlet.ServletException;
import com.microtomcat.session.SessionManager;
import com.microtomcat.context.SimpleServletContext;
import javax.servlet.http.HttpServletResponse;

public class HttpServer extends AbstractHttpServer {
    private static final int DEFAULT_PORT = 8080;
    private final int port;
    private static final String WEB_ROOT = "webroot";
    private final ExecutorService executorService;
    private Context context;
    private SessionManager sessionManager;
    private volatile ServerSocket serverSocket;

    public HttpServer(int port) throws IOException {
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
             OutputStream output = socket.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {

            // 读取请求行
            String requestLine = reader.readLine();
            if (requestLine == null) {
                return;
            }

            // 解析请求
            String[] parts = requestLine.split(" ");
            if (parts.length != 3) {
                return;
            }

            // 创建 Request 和 Response 对象
            Request request = new Request(input, sessionManager);
            // 解析请求
            request.parse();

            Response response = new Response(output);
            
            // 如果有 Context，使用 Context 处理请求
            if (context != null) {
                try {
                    request.setContext(context);
                    context.service(request, response);
                    // 确保状态码被设置并且响应被完全写入
                    if (!response.isCommitted()) {
                        response.setStatus(HttpServletResponse.SC_OK);
                    }
                    response.flushBuffer();
                    return;
                } catch (ServletException e) {
                    log("Error processing request through context: " + e.getMessage());
                    e.printStackTrace();
                    // 发送500错误响应
                    String errorMessage = "500 Internal Server Error";
                    output.write("HTTP/1.1 500 Internal Server Error\r\n".getBytes());
                    output.write("Content-Type: text/plain\r\n".getBytes());
                    output.write(("Content-Length: " + errorMessage.length() + "\r\n").getBytes());
                    output.write("\r\n".getBytes());
                    output.write(errorMessage.getBytes());
                    output.flush();
                    return;
                }
            }

            // 如果 Context 没有处理请求，返回 404
            String uri = parts[1];
            String errorMessage = "404 Not Found: " + uri;
            output.write("HTTP/1.1 404 Not Found\r\n".getBytes());
            output.write("Content-Type: text/plain\r\n".getBytes());
            output.write(("Content-Length: " + errorMessage.length() + "\r\n").getBytes());
            output.write("\r\n".getBytes());
            output.write(errorMessage.getBytes());
            output.flush();
            log("Request not handled: " + uri);
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
            ServerConfig config = new ServerConfig(
                port,           // 使用解析的端口
                false,          // 使用阻塞式 IO
                10,            // 线程池大小
                WEB_ROOT       // Web根目录
            );
            
            AbstractHttpServer server = HttpServerFactory.createServer(config);
            server.init();
            server.start();
        } catch (IOException | LifecycleException e) {
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
}
