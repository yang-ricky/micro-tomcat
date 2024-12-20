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

import com.microtomcat.server.ServerConfig;
import com.microtomcat.server.AbstractHttpServer;
import com.microtomcat.server.HttpServerFactory;

public class HttpServer {
    private static final int DEFAULT_PORT = 8080;
    private final int port;
    private static final String WEB_ROOT = "webroot";
    private final ExecutorService executorService;

    public HttpServer(int port) {
        this.port = port;
        this.executorService = Executors.newFixedThreadPool(10);
    }

    private void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME);
        System.out.printf("[%s] %s%n", timestamp, message);
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log("Server started on port: " + port);
            
            while (true) {
                Socket socket = serverSocket.accept();
                log("New connection accepted from: " + socket.getInetAddress());
                executorService.submit(() -> {
                    try {
                        handleRequest(socket);
                    } catch (IOException e) {
                        log("Error handling request: " + e.getMessage());
                    } finally {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            log("Error closing socket: " + e.getMessage());
                        }
                    }
                });
            }
        } catch (IOException e) {
            log("Server error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            executorService.shutdown();
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
                String errorMessage = "404 File Not Found";
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

    public static void main(String[] args) {
        try {
            // 创建服务器配置
            ServerConfig config = new ServerConfig(
                DEFAULT_PORT,    // 端口
                true,          // 使用阻塞式 IO
                10,            // 线程池大小
                WEB_ROOT       // Web根目录
            );
            
            // 通过工厂创建服务器实例
            AbstractHttpServer server = HttpServerFactory.createServer(config);
            
            // 启动服务器
            server.start();
        } catch (IOException e) {
            System.err.println("Server startup failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
