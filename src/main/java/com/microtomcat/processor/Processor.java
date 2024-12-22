package com.microtomcat.processor;

import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import com.microtomcat.context.Context;
import com.microtomcat.context.ContextManager;
import com.microtomcat.lifecycle.LifecycleBase;
import com.microtomcat.pipeline.Pipeline;
import com.microtomcat.pipeline.StandardPipeline;
import com.microtomcat.pipeline.valve.AccessLogValve;
import com.microtomcat.pipeline.valve.AuthenticatorValve;
import com.microtomcat.pipeline.valve.StandardValve;
import com.microtomcat.servlet.Servlet;
import com.microtomcat.servlet.ServletException;
import com.microtomcat.servlet.ServletLoader;
import com.microtomcat.session.SessionManager;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.microtomcat.lifecycle.LifecycleException;
public class Processor extends LifecycleBase {
    private final String webRoot;
    private final ServletLoader servletLoader;
    private final SessionManager sessionManager;
    private final ContextManager contextManager;
    private final StandardPipeline pipeline;

    public Processor(String webRoot, ServletLoader servletLoader, 
                    SessionManager sessionManager, ContextManager contextManager) {
        this.webRoot = webRoot;
        this.servletLoader = servletLoader;
        this.sessionManager = sessionManager;
        this.contextManager = contextManager;
        
        this.pipeline = new StandardPipeline();
        
        // 添加基础阀门
        pipeline.addValve(new AccessLogValve());
        pipeline.addValve(new AuthenticatorValve());
        
        // 设置基础阀门处理请求
        pipeline.setBasic(new StandardValve(webRoot, servletLoader));
    }

    @Override
    protected void initInternal() throws LifecycleException {
        log("Initializing processor");
    }

    @Override
    protected void startInternal() throws LifecycleException {
        log("Starting processor");
    }

    @Override
    protected void stopInternal() throws LifecycleException {
        log("Stopping processor");
    }

    @Override
    protected void destroyInternal() throws LifecycleException {
        log("Destroying processor");
    }

    private void log(String message) {
        System.out.println("[Processor] " + message);
    }

    public void process(Socket socket) {
        try {
            Request request = new Request(socket.getInputStream(), sessionManager);
            Response response = new Response(socket.getOutputStream(), request);
            
            request.parse();
            Context context = contextManager.getContext(request.getUri());
            request.setContext(context);
            
            // 使用 pipeline 处理请求
            pipeline.invoke(request, response);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 