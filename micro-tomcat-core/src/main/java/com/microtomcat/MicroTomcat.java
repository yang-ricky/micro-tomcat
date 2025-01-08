package com.microtomcat;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.microtomcat.server.ServerConfig;
import com.microtomcat.server.AbstractHttpServer;
import com.microtomcat.lifecycle.LifecycleException;
import com.microtomcat.container.Context;
import com.microtomcat.session.SessionManager;
import com.microtomcat.context.SimpleServletContext;
import com.microtomcat.servlet.DefaultServlet;

import javax.servlet.Servlet;
import com.microtomcat.protocol.Protocol;
import com.microtomcat.protocol.Http11Protocol;

//TODO: connector -> protocol handler -> protocol

public class MicroTomcat extends AbstractHttpServer {
    private static final int DEFAULT_PORT = 8080;
    private final int port;
    private static final String WEB_ROOT = "webroot";
    private final ExecutorService executorService;
    private Context context;
    private SessionManager sessionManager;
    private Protocol protocol;

    public MicroTomcat(int port) throws IOException {
        super(new ServerConfig(port, false, 10, WEB_ROOT));
        this.port = port;
        this.executorService = Executors.newFixedThreadPool(10);
        this.sessionManager = new SessionManager(new SimpleServletContext(""));
        
        Http11Protocol http11Protocol = new Http11Protocol();
        http11Protocol.setSessionManager(sessionManager);
        http11Protocol.setPort(port);
        this.protocol = http11Protocol;
    }

    public void setContext(Context context) {
        this.context = context;
        if (protocol instanceof Http11Protocol) {
            ((Http11Protocol) protocol).setContext(context);
        }
    }

    @Override
    protected void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME);
        System.out.printf("[HttpServer %s] %s%n", timestamp, message);
    }

    @Override
    public void start() throws LifecycleException {
        try {
            // 启动协议处理器
            protocol.init();
            protocol.start();
            
        } catch (Exception e) {
            throw new LifecycleException("Failed to start server", e);
        }
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
            // 创建并配置 MicroTomcat
            MicroTomcat tomcat = new MicroTomcat(port);
            
            // 创建 Context
            String contextPath = "";
            String docBase = "webroot";
            Context context = tomcat.addContext(contextPath, docBase);
            
            // 添加 DefaultServlet 处理静态资源
            DefaultServlet defaultServlet = new DefaultServlet();
            tomcat.addServlet(context, "default", defaultServlet);
            tomcat.addServletMapping(context, "/*", "default");
            
            // 启动服务器
            tomcat.init();
            tomcat.start();
            
            // 等待服务器停止
            Thread.currentThread().join();
            
        } catch (Exception e) {
            System.err.println("Server startup failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void stop() throws LifecycleException {
        try {
            if (protocol != null) {
                protocol.stop();
            }
        } catch (Exception e) {
            log("Error while stopping server: " + e.getMessage());
        }
        
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

    public Context addContext(String contextPath, String docBase) throws IOException {
        if (contextPath == null) {
            throw new IllegalArgumentException("contextPath cannot be null");
        }
        if (docBase == null) {
            throw new IllegalArgumentException("docBase cannot be null");
        }
        Context newContext = new Context(contextPath, docBase);
        this.context = newContext;
        this.setContext(context);
        return newContext;
    }

    public void addServlet(Context context, String servletName, Servlet servlet) {
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        if (servletName == null) {
            throw new IllegalArgumentException("servletName cannot be null");
        }
        if (servlet == null) {
            throw new IllegalArgumentException("servlet cannot be null");
        }
        context.addServlet(servletName, servlet);
    }

    public void addServletMapping(Context context, String urlPattern, String servletName) {
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        if (urlPattern == null) {
            throw new IllegalArgumentException("urlPattern cannot be null");
        }
        if (servletName == null) {
            throw new IllegalArgumentException("servletName cannot be null");
        }
        context.addServletMapping(urlPattern, servletName);
    }

    public void addServletMappingDecoded(Context context, String urlPattern, String servletName) {
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        if (urlPattern == null) {
            throw new IllegalArgumentException("urlPattern cannot be null");
        }
        if (servletName == null) {
            throw new IllegalArgumentException("servletName cannot be null");
        }
        context.addServletMapping(urlPattern, servletName);
    }

    public Protocol getProtocol() {
        return protocol;
    }
}
