package com.microtomcat.server;

import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlockingHttpServer extends AbstractHttpServer {
    private final ExecutorService executorService;

    public BlockingHttpServer(ServerConfig config) {
        super(config);
        this.executorService = Executors.newFixedThreadPool(config.getThreadPoolSize());
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
        try (InputStream input = socket.getInputStream();
             OutputStream output = socket.getOutputStream()) {
            
            // Create and parse request
            Request request = new Request(input);
            request.parse();
            
            // Log request details
            log(String.format("Received %s request for URI: %s", 
                request.getMethod(), request.getUri()));
            
            // Create and send response
            Response response = new Response(output, request);
            response.sendStaticResource();
            
        } catch (IOException e) {
            log("Error handling request: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                log("Error closing socket: " + e.getMessage());
            }
        }
    }
} 