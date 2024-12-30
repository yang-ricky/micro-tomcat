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

public class HttpServer extends AbstractHttpServer {
    private static final int DEFAULT_PORT = 8080;
    private final int port;
    private static final String WEB_ROOT = "webroot";
    private final ExecutorService executorService;

    public HttpServer(int port) throws IOException {
        super(new ServerConfig(port, false, 10, WEB_ROOT));
        this.port = port;
        this.executorService = Executors.newFixedThreadPool(10);
    }

    @Override
    protected void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME);
        System.out.printf("[HttpServer %s] %s%n", timestamp, message);
    }

    @Override
    public void start() throws LifecycleException {
        try {
            // 初始化类加载器
            ClassLoaderManager.init();
            
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                log("Server started on port: " + port);
                while (!Thread.currentThread().isInterrupted()) {
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
                }
            }
        } catch (Exception e) {
            throw new LifecycleException("Failed to start server", e);
        }
    }

    private void handleRequest(Socket socket) throws IOException {
        try (InputStream input = socket.getInputStream();
             OutputStream output = socket.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {

            // 读取请求行
            String requestLine = reader.readLine();
            if (requestLine == null) {
                return;
            }

            // 解析URI
            String[] parts = requestLine.split(" ");
            if (parts.length != 3) {
                return;
            }
            String uri = parts[1];
            log("Received request for URI: " + uri);

            // 读取文件
            Path filePath = Paths.get(WEB_ROOT, uri);
            if (Files.exists(filePath)) {
                // 发送响应头
                String contentType = getContentType(uri);
                byte[] fileContent = Files.readAllBytes(filePath);
                
                output.write("HTTP/1.1 200 OK\r\n".getBytes());
                output.write(("Content-Type: " + contentType + "\r\n").getBytes());
                output.write(("Content-Length: " + fileContent.length + "\r\n").getBytes());
                output.write("\r\n".getBytes());
                
                // 发送文件内容
                output.write(fileContent);
                output.flush();
                
                log("Successfully served file: " + uri);
            } else {
                // 文件不存在，返回404
                String errorMessage = "404 File Not Found: " + uri;
                output.write("HTTP/1.1 404 Not Found\r\n".getBytes());
                output.write("Content-Type: text/plain\r\n".getBytes());
                output.write(("Content-Length: " + errorMessage.length() + "\r\n").getBytes());
                output.write("\r\n".getBytes());
                output.write(errorMessage.getBytes());
                output.flush();
                
                log("File not found: " + uri);
            }
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
        // 关闭线程池
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
