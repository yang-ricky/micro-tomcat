package com.microtomcat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class HttpServer {
    private static final int DEFAULT_PORT = 8080;
    private final int port;

    public HttpServer(int port) {
        this.port = port;
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
                // TODO: Handle request
            }
        } catch (IOException e) {
            log("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        HttpServer server = new HttpServer(DEFAULT_PORT);
        server.start();
    }
}
