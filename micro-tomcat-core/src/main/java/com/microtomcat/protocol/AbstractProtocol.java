package com.microtomcat.protocol;

import java.net.ServerSocket;
import java.net.InetSocketAddress;
import java.io.IOException;

public abstract class AbstractProtocol implements Protocol {
    protected int port;
    
    @Override
    public void setPort(int port) {
        this.port = port;
    }
    
    @Override
    public int getPort() {
        return port;
    }
    
    @Override
    public boolean isPortAvailable() {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(port), 1);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
} 