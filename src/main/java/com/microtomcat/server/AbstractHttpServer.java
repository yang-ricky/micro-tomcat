package com.microtomcat.server;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class AbstractHttpServer {
    protected final ServerConfig config;
    protected static final String WEB_ROOT = "webroot";
    
    protected AbstractHttpServer(ServerConfig config) {
        this.config = config;
    }
    
    public abstract void start() throws IOException;
    protected abstract void stop();
    
    protected void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME);
        System.out.printf("[%s] %s%n", timestamp, message);
    }
} 