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
import com.microtomcat.container.Engine;

public class Processor extends LifecycleBase {
    private final String webRoot;
    private final Engine engine;
    private final Pipeline pipeline;

    public Processor(String webRoot, Engine engine) {
        this.webRoot = webRoot;
        this.engine = engine;
        
        this.pipeline = new StandardPipeline();
        pipeline.addValve(new AccessLogValve());
        pipeline.addValve(new AuthenticatorValve());
        pipeline.setBasic(new StandardValve(webRoot));
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
            Request request = new Request(socket.getInputStream(), null);
            Response response = new Response(socket.getOutputStream(), request);
            
            request.parse();
            
            // 直接使用Engine处理请求
            engine.invoke(request, response);
            
        } catch (Exception e) {
            log("Error processing request: " + e.getMessage());
        }
    }
} 