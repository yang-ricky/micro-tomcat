package com.microtomcat.connector;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import com.microtomcat.processor.ProcessorPool;
import com.microtomcat.lifecycle.LifecycleBase;
import com.microtomcat.lifecycle.LifecycleException;
import javax.management.StandardMBean;

public class Connector extends LifecycleBase implements Runnable, ConnectorMBean {
    private final ServerSocket serverSocket;
    private final ProcessorPool processorPool;
    private volatile boolean running = true;
    private final BlockingQueue<Socket> connectionQueue;
    private final Object lock = new Object();
    private final ExecutorService executorService;
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    private int connectionTimeout = 20000; // 默认20秒
    
    public Connector(int port, ProcessorPool processorPool) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.processorPool = processorPool;
        this.connectionQueue = new ArrayBlockingQueue<>(200);
        this.executorService = processorPool.getExecutor();
    }
    
    @Override
    protected void initInternal() throws LifecycleException {
        log("Initializing Connector on port: " + serverSocket.getLocalPort());
        if (serverSocket == null || serverSocket.isClosed()) {
            throw new LifecycleException("Server socket not initialized or closed");
        }
    }
    
    @Override
    protected void startInternal() throws LifecycleException {
        log("Starting Connector");
        running = true;
        
        // 启动工作线程处理连接队列
        Thread worker = new Thread(() -> {
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
        }, "Connector-Worker");
        worker.start();
    }
    
    @Override
    protected void stopInternal() throws LifecycleException {
        log("Stopping Connector");
        running = false;
        try {
            // 清理连接队列
            connectionQueue.clear();
            // 关闭服务器套接字
            if (!serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            throw new LifecycleException("Failed to stop connector", e);
        }
    }
    
    @Override
    protected void destroyInternal() throws LifecycleException {
        log("Destroying Connector");
        try {
            if (!serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            throw new LifecycleException("Failed to destroy connector", e);
        }
    }
    
    private void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME);
        System.out.printf("[Connector %s] %s%n", timestamp, message);
    }
    
    @Override
    public void run() {
       // 不知道为什么不需要实现
    }

    public void processSocket(Socket socket) {
        synchronized(lock) {
            while (connectionQueue.remainingCapacity() == 0) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            connectionQueue.offer(socket);
            lock.notifyAll();
        }
    }
    
    public Socket getSocket() {
        synchronized(lock) {
            while (connectionQueue.isEmpty()) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            Socket socket = connectionQueue.poll();
            lock.notifyAll();
            return socket;
        }
    }
    
    @Override
    public boolean isRunning() {
        return running;
    }
    
    @Override
    public int getCurrentThreadCount() {
        return ((ThreadPoolExecutor) executorService).getActiveCount();
    }
    
    @Override
    public int getActiveConnections() {
        return activeConnections.get();
    }
    
    @Override
    public long getTotalRequests() {
        return totalRequests.get();
    }
    
    @Override
    public double getAverageResponseTime() {
        long requests = totalRequests.get();
        return requests > 0 ? (double) totalProcessingTime.get() / requests : 0;
    }
    
    @Override
    public void setMaxThreads(int maxThreads) {
        if (executorService instanceof ThreadPoolExecutor) {
            ((ThreadPoolExecutor) executorService).setMaximumPoolSize(maxThreads);
        }
    }
    
    @Override
    public void setConnectionTimeout(int timeout) {
        this.connectionTimeout = timeout;
    }
}