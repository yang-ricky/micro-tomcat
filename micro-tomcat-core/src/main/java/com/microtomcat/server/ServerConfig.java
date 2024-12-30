package com.microtomcat.server;

public class ServerConfig {
    private final int port;
    private final boolean nonBlocking;
    private final int threadPoolSize;
    
    private final String webRoot;
    
    public ServerConfig(int port, boolean nonBlocking, int threadPoolSize, String webRoot) {
        this.port = port;
        this.nonBlocking = nonBlocking;
        this.threadPoolSize = threadPoolSize;
        this.webRoot = webRoot;
    }
    
    public boolean isNonBlocking() {
        return nonBlocking;
    }
    
    public int getPort() {
        return port;
    }
    
    public String getWebRoot() {
        return webRoot;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }
    
    // getters...
} 