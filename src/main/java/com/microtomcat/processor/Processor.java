package com.microtomcat.processor;

import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import com.microtomcat.context.Context;
import com.microtomcat.context.ContextManager;
import com.microtomcat.servlet.Servlet;
import com.microtomcat.servlet.ServletException;
import com.microtomcat.servlet.ServletLoader;
import com.microtomcat.session.SessionManager;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Processor {
    private final String webRoot;
    private final ServletLoader servletLoader;
    private final SessionManager sessionManager;
    private final ContextManager contextManager;

    public Processor(String webRoot, ServletLoader servletLoader, 
                    SessionManager sessionManager, ContextManager contextManager) {
        this.webRoot = webRoot;
        this.servletLoader = servletLoader;
        this.sessionManager = sessionManager;
        this.contextManager = contextManager;
    }

    public void process(Socket socket) {
        try {
            Request request = new Request(socket.getInputStream(), sessionManager);
            Response response = new Response(socket.getOutputStream(), request);
            
            // 解析请求
            request.parse();
            
            // 获取请求对应的上下文
            Context context = contextManager.getContext(request.getUri());
            if (context == null) {
                log("No context found for URI: " + request.getUri());
                response.sendError(404, "Application context not found");
                return;
            }
            
            // 设置请求的上下文信息
            request.setContext(context);
            
            // 处理请求
            String uri = request.getUri();
            String contextPath = context.getContextPath();
            
            log("Processing request - URI: " + uri + ", Context Path: " + contextPath);
            
            // 如果是根上下文，直接使用原始URI
            if (contextPath.isEmpty()) {
                if (uri.startsWith("/servlet/")) {
                    handleServlet(request, response, context, uri);
                } else {
                    handleStatic(request, response, context, uri);
                }
            } else {
                // 如果是应用上下文，去掉上下文路径前缀
                String pathInContext = uri.substring(contextPath.length());
                if (pathInContext.startsWith("/servlet/")) {
                    handleServlet(request, response, context, pathInContext);
                } else {
                    handleStatic(request, response, context, pathInContext);
                }
            }
        } catch (Exception e) {
            log("Error processing request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleServlet(Request request, Response response, Context context, String uri) {
        try {
            log("Loading servlet for URI: " + uri);
            Servlet servlet = context.getServletLoader().loadServlet(uri);
            if (servlet == null) {
                log("Servlet not found for URI: " + uri);
                response.sendError(404, "Servlet not found");
                return;
            }
            
            log("Servlet loaded successfully, calling service method");
            servlet.service(request, response);
            log("Servlet service completed");
        } catch (Exception e) {
            log("Error in servlet processing: " + e.getMessage());
            e.printStackTrace();
            try {
                response.sendError(500, "Servlet Error: " + e.getMessage());
            } catch (IOException ioe) {
                log("Error sending error response: " + ioe.getMessage());
            }
        }
    }

    private void handleStatic(Request request, Response response, Context context, String uri) throws IOException {
        log("Processing static resource: " + uri);
        response.sendStaticResource();
    }

    protected void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME);
        System.out.printf("[%s] [Processor-%d] %s%n", timestamp, hashCode() % 100, message);
    }
} 