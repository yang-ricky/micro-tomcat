package com.microtomcat.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.InetAddress;

public interface ServerSocketFactory {
    ServerSocket createSocket(int port) throws IOException;
    ServerSocket createSocket(int port, int backlog) throws IOException;
    ServerSocket createSocket(int port, int backlog, InetAddress ifAddress) throws IOException;
} 