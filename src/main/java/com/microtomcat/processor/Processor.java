package com.microtomcat.processor;

import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import com.microtomcat.servlet.Servlet;
import com.microtomcat.servlet.ServletException;
import com.microtomcat.servlet.ServletLoader;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Processor {
    private final ServletLoader servletLoader;
    private final String webRoot;
    private boolean available = true;

    public Processor(String webRoot, ServletLoader servletLoader) {
        this.webRoot = webRoot;
        this.servletLoader = servletLoader;
    }

    public void process(Socket socket) {
        try (InputStream input = socket.getInputStream();
             OutputStream output = socket.getOutputStream()) {
            
            Request request = new Request(input);
            Response response = new Response(output, request);
            
            request.parse();
            String uri = request.getUri();
            
            log(String.format("Received %s request for URI: %s from %s", 
                request.getMethod(), uri, socket.getInetAddress()));

            if (uri == null) {
                response.sendError(400, "Bad Request: Missing URI");
                return;
            }

            if (uri.startsWith("/servlet/")) {
                processServlet(request, response, uri);
            } else {
                log("Processing static resource: " + uri);
                response.sendStaticResource();
            }
            
        } catch (IOException e) {
            log("Error processing request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processServlet(Request request, Response response, String uri) throws IOException {
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
    }

    protected void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME);
        System.out.printf("[%s] [Processor-%d] %s%n", timestamp, hashCode() % 100, message);
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
} 