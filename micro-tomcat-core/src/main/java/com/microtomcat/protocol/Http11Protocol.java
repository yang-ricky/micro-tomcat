package com.microtomcat.protocol;

import com.microtomcat.processor.ProcessorPool;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.BindException;
import javax.servlet.http.HttpServletResponse;
import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import com.microtomcat.container.Context;
import com.microtomcat.container.Engine;
import com.microtomcat.session.SessionManager;
import com.microtomcat.loader.ClassLoaderManager;
import com.microtomcat.lifecycle.LifecycleException;
import com.microtomcat.net.ServerSocketFactory;
import com.microtomcat.net.DefaultServerSocketFactory;

public class Http11Protocol extends AbstractProtocol {
    // HTTP/1.1 specific constants
    private static final String HTTP_11 = "HTTP/1.1";
    private static final int DEFAULT_MAX_HEADER_SIZE = 8 * 1024;
    private static final int DEFAULT_MAX_POST_SIZE = 1 * 1024 * 1024;
    private static final int DEFAULT_SOCKET_TIMEOUT = 10000;
    private static final int DEFAULT_KEEP_ALIVE_TIMEOUT = 15000;
    private static final int DEFAULT_THREAD_POOL_SIZE = 50;
    private static final int DEFAULT_MAX_CONNECTIONS = 1000;
    
    private static final int CORE_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 50;
    private static final int QUEUE_CAPACITY = 30;
    private static final long KEEP_ALIVE_TIME = 30L;

    private ServerSocket serverSocket;
    private final ThreadPoolExecutor executorService;
    private ProcessorPool processorPool;
    private volatile boolean running;
    private final BlockingQueue<Runnable> taskQueue;
    private final AtomicInteger currentConnections = new AtomicInteger(0);
    private Engine engine;
    private SessionManager sessionManager;
    private Context context;
    private final ServerSocketFactory serverSocketFactory;
    //TODO: Connector -> ProtocolHandler -> Endpoint -> ServerSocketFactory
    public Http11Protocol() {
        // 使用有界队列和合理的线程池配置
        this.taskQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        this.executorService = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            taskQueue,
            new ThreadPoolExecutor.CallerRunsPolicy()  // 使用调用者运行策略而不是直接拒绝
        );
        this.serverSocketFactory = new DefaultServerSocketFactory();
    }

    @Override
    public void init() throws Exception {
        log("Initializing HTTP/1.1 protocol on port " + port);
        
        if (!isPortAvailable()) {
            log("Port " + port + " is already in use");
            throw new BindException("Port " + port + " is already in use");
        }
        
        log("Creating server socket...");
        serverSocket = serverSocketFactory.createSocket(port);
        log("Server socket successfully bound to port " + port);
    }

    @Override
    public void start() throws Exception {
        try {
            ClassLoaderManager.init();

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

    public void handleRequest(Socket socket) throws IOException {
        try (InputStream input = socket.getInputStream();
             OutputStream output = socket.getOutputStream()) {
            
            Request request = new Request(input, sessionManager);
            Response response = new Response(output);
            
            request.parse();
            
            log("Handling request: " + request.getRequestURI());
            
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


    @Override
    public void stop() throws Exception {
        running = false;
        if (serverSocket != null) {
            serverSocket.close();
        }
        executorService.shutdown();
        if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
        }
    }

    @Override
    public void setProcessorPool(ProcessorPool processorPool) {
        this.processorPool = processorPool;
    }

    private void log(String message) {
        System.out.printf("[Http11Protocol] %s%n", message);
    }

    private String readLine(InputStream input) throws IOException {
        StringBuilder line = new StringBuilder();
        int c;
        while ((c = input.read()) != -1) {
            if (c == '\n') {
                if (line.length() > 0 && line.charAt(line.length() - 1) == '\r') {
                    line.setLength(line.length() - 1);
                }
                return line.toString();
            }
            line.append((char) c);
        }
        return null;
    }

    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void setContext(Context context) {
        this.context = context;
    }
} 