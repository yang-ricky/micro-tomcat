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
        // Convert URL path to file system path
        String filePath = config.getWebRoot() + requestPath;
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            sendError(outputStream, 404, "Not Found");
            return;
        }

        // Send successful response with file contents
        byte[] fileContent = Files.readAllBytes(path);
        String contentType = Files.probeContentType(path);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        String response = "HTTP/1.1 200 OK\r\n" +
                         "Content-Type: " + contentType + "\r\n" +
                         "Content-Length: " + fileContent.length + "\r\n" +
                         "\r\n";
        
        outputStream.write(response.getBytes());
        outputStream.write(fileContent);
        outputStream.flush();
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