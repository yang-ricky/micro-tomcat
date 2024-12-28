package com.microtomcat.server;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.microtomcat.container.Engine;
import com.microtomcat.lifecycle.LifecycleException;
import com.microtomcat.processor.ProcessorPool;

public abstract class AbstractHttpServer {
    protected final ServerConfig config;
    protected static final String WEB_ROOT = "webroot";
    protected ProcessorPool processorPool;
    protected Engine engine;
    
    protected AbstractHttpServer(ServerConfig config) {
        this.config = config;
    }
    
    public abstract void start() throws LifecycleException;
    public abstract void stop() throws LifecycleException;
    
    protected void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME);
        System.out.printf("[%s] %s%n", timestamp, message);
    }
    
    protected void initInternal() throws LifecycleException {
        // 基础初始化逻辑
    }
    
    protected void destroyInternal() throws LifecycleException {
        // 基础销毁逻辑
    }
    
    public void init() throws LifecycleException {
        initInternal();
    }
} 