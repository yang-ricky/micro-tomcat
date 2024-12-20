package com.microtomcat.server;

import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import com.microtomcat.servlet.Servlet;
import com.microtomcat.servlet.ServletException;
import com.microtomcat.servlet.ServletLoader;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlockingHttpServer extends AbstractHttpServer {
    private final ExecutorService executorService;
    private final ServletLoader servletLoader;

    public BlockingHttpServer(ServerConfig config) {
        super(config);
        this.executorService = Executors.newFixedThreadPool(config.getThreadPoolSize());
        try {
            // 假设 webRoot 是 "webroot" 目录
            this.servletLoader = new ServletLoader(config.getWebRoot(), "target/classes");
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize ServletLoader", e);
        }
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
        servletLoader.destroy();
        executorService.shutdown();
    }
    
    protected void handleRequest(Socket socket) {
        try (InputStream input = socket.getInputStream();
             OutputStream output = socket.getOutputStream()) {
            
            Request request = new Request(input);
            request.parse();
            Response response = new Response(output, request);
            
            String uri = request.getUri();
            log(String.format("Received %s request for URI: %s", 
                request.getMethod(), uri));
            
            if (uri.startsWith("/servlet/")) {
                try {
                    log("Loading servlet for URI: " + uri);
                    Servlet servlet = servletLoader.loadServlet(uri);
                    log("Servlet loaded successfully, calling service method");
                    try {
                        servlet.service(request, response);
                        log("Servlet service completed");
                    } catch (Exception e) {
                        log("Error in servlet service: " + e.getMessage());
                        e.printStackTrace();
                        response.sendError(500, "Servlet Error: " + e.getMessage());
                    }
                } catch (ServletException e) {
                    log("Servlet loading error: " + e.getMessage());
                    e.printStackTrace();
                    response.sendError(500, "Servlet Error: " + e.getMessage());
                }
            } else {
                response.sendStaticResource();
            }
            
        } catch (IOException e) {
            log("Error handling request: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                log("Error closing socket: " + e.getMessage());
            }
        }
    }
} 