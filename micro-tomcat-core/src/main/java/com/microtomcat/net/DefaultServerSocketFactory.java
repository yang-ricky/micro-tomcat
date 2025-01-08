package com.microtomcat.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class DefaultServerSocketFactory implements ServerSocketFactory {
    @Override
    public ServerSocket createSocket(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(port), 200);
        return serverSocket;
    }

    @Override
    public ServerSocket createSocket(int port, int backlog) throws IOException {
        ServerSocket serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(port), backlog);
        return serverSocket;
    }

    @Override
    public ServerSocket createSocket(int port, int backlog, InetAddress ifAddress) throws IOException {
        ServerSocket serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(ifAddress, port), backlog);
        return serverSocket;
    }
} 