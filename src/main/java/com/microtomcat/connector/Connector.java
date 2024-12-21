package com.microtomcat.connector;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import com.microtomcat.processor.ProcessorPool;

public class Connector implements Runnable {
    private final ServerSocket serverSocket;
    private final ProcessorPool processorPool;
    private volatile boolean running = true;
    private final BlockingQueue<Socket> connectionQueue;
    
    public Connector(int port, ProcessorPool processorPool) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.processorPool = processorPool;
        this.connectionQueue = new ArrayBlockingQueue<>(200);
    }
    
    @Override
    public void run() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                processSocket(socket);
            } catch (IOException e) {
                if (running) {
                    log("Error accepting connection: " + e.getMessage());
                }
            }
        }
    }

    public void processSocket(Socket socket) {
        synchronized(connectionQueue) {
            while (connectionQueue.remainingCapacity() == 0) {
                try {
                    connectionQueue.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            connectionQueue.offer(socket);
            connectionQueue.notifyAll();
        }
    }
    
    public Socket getSocket() {
        synchronized(connectionQueue) {
            while (connectionQueue.isEmpty()) {
                try {
                    connectionQueue.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            Socket socket = connectionQueue.poll();
            connectionQueue.notifyAll();
            return socket;
        }
    }
    
    public void close() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            log("Error closing server socket: " + e.getMessage());
        }
    }

    private void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        System.err.printf("[%s] [Connector] %s%n", timestamp, message);
    }
}