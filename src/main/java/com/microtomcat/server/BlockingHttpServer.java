package com.microtomcat.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlockingHttpServer extends AbstractHttpServer {
    private final ExecutorService executorService;

    public BlockingHttpServer(ServerConfig config) {
        super(config);
        this.executorService = Executors.newFixedThreadPool(10); // Using default thread pool size since getter is not defined
    }

    @Override
    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(config.getPort())) {
            log("Blocking server started on port: " + config.getPort());
            
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();
                log("New connection accepted from: " + socket.getInetAddress());
                executorService.submit(() -> handleRequest(socket));
            }
        } finally {
            stop();
        }
    }

    @Override
    protected void stop() {
        executorService.shutdown();
    }
    
    protected void handleRequest(Socket socket) {
        try (
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             OutputStream outputStream = socket.getOutputStream()) {
            
            // Read the request line
            String requestLine = reader.readLine();
            if (requestLine == null) {
                return;
            }

            // Parse request line
            String[] requestParts = requestLine.split(" ");
            if (requestParts.length != 3) {
                sendError(outputStream, 400, "Bad Request");
                return;
            }

            String method = requestParts[0];
            String path = requestParts[1];
            
            // Currently only handling GET requests
            if (!"GET".equals(method)) {
                sendError(outputStream, 405, "Method Not Allowed");
                return;
            }

            // Serve the file
            serveFile(path, outputStream);

        } catch (IOException e) {
            log("Error handling request: " + e.getMessage());
        }
    }


    private void serveFile(String requestPath, OutputStream outputStream) throws IOException {
        String filePath = config.getWebRoot() + requestPath;
        Path path = Paths.get(filePath);
        
        if (!Files.exists(path)) {
            sendError(outputStream, 404, "Not Found");
            return;
        }

        byte[] fileContent = Files.readAllBytes(path);
        String contentType = getContentType(path);
        
        String response = "HTTP/1.1 200 OK\r\n" +
                         "Content-Type: " + contentType + "; charset=UTF-8\r\n" +
                         "Content-Length: " + fileContent.length + "\r\n" +
                         "\r\n";
        
        outputStream.write(response.getBytes());
        outputStream.write(fileContent);
        outputStream.flush();
    }

    private String getContentType(Path path) {
        String fileName = path.toString().toLowerCase();
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return "text/html";
        } else if (fileName.endsWith(".css")) {
            return "text/css";
        } else if (fileName.endsWith(".js")) {
            return "text/javascript";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.endsWith(".txt")) {
            return "text/plain";
        } else if (fileName.endsWith(".pdf")) {
            return "application/pdf";
        } else {
            // Try to probe content type first
            try {
                String probed = Files.probeContentType(path);
                if (probed != null) {
                    return probed;
                }
            } catch (IOException e) {
                log("Error probing content type: " + e.getMessage());
            }
            // Default to text/plain for unknown types instead of application/octet-stream
            return "text/plain";
        }
    }


    private void sendError(OutputStream outputStream, int statusCode, String message) throws IOException {
        String response = "HTTP/1.1 " + statusCode + " " + message + "\r\n" +
                         "Content-Type: text/plain\r\n" +
                         "Content-Length: " + message.length() + "\r\n" +
                         "\r\n" +
                         message;
        outputStream.write(response.getBytes());
        outputStream.flush();
    }
} 