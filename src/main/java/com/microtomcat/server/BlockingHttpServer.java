package com.microtomcat.server;

import com.microtomcat.connector.Connector;
import com.microtomcat.processor.Processor;
import com.microtomcat.processor.ProcessorPool;
import com.microtomcat.servlet.ServletLoader;
import com.microtomcat.session.SessionManager;
import com.microtomcat.context.ContextManager;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlockingHttpServer extends AbstractHttpServer {
    private final ExecutorService executorService;
    private final ProcessorPool processorPool;
    private final Connector connector;
    private volatile boolean running = true;
    private final ContextManager contextManager;

    public BlockingHttpServer(ServerConfig config) {
        super(config);
        this.executorService = Executors.newFixedThreadPool(config.getThreadPoolSize());
        this.contextManager = new ContextManager(config.getWebRoot());
        
        // 添加根上下文，处理不带应用前缀的请求
        contextManager.createContext("", config.getWebRoot());  // 根上下文
        
        // 添加应用上下文
        contextManager.createContext("/app1", config.getWebRoot() + "/app1");
        contextManager.createContext("/app2", config.getWebRoot() + "/app2");
        
        try {
            this.processorPool = new ProcessorPool(
                100,
                config.getWebRoot(),
                contextManager
            );
            this.connector = new Connector(config.getPort(), processorPool);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize server", e);
        }
    }

    @Override
    public void start() throws IOException {
        log("Blocking server started on port: " + config.getPort());
        
        // 启动连接处理线程
        for (int i = 0; i < config.getThreadPoolSize(); i++) {
            executorService.submit(new ConnectionHandler());
        }
        
        // 启动连接接收线程
        connector.run();
    }

    private class ConnectionHandler implements Runnable {
        @Override
        public void run() {
            while (running) {
                Socket socket = connector.getSocket();
                if (socket != null) {
                    handleRequest(socket);
                }
            }
        }
    }

    private void handleRequest(Socket socket) {
        Processor processor = null;
        try {
            processor = processorPool.getProcessor(5000); // 5秒超时
            if (processor != null) {
                log(String.format("Acquired processor (active/total: %d/%d) for request from: %s",
                    processorPool.getActiveCount(),
                    processorPool.getTotalCount(),
                    socket.getInetAddress()));
                processor.process(socket);
            } else {
                log("No processor available, sending 503 response to: " + socket.getInetAddress());
                try (OutputStream output = socket.getOutputStream()) {
                    String response = "HTTP/1.1 503 Service Unavailable\r\n" +
                            "Content-Type: text/plain\r\n" +
                            "Content-Length: 35\r\n" +
                            "\r\n" +
                            "Server is too busy, please try later.";
                    output.write(response.getBytes());
                }
            }
        } catch (InterruptedException e) {
            log("Processor acquisition interrupted for " + socket.getInetAddress() + ": " + e.getMessage());
        } catch (IOException e) {
            log("Error processing request from " + socket.getInetAddress() + ": " + e.getMessage());
        } finally {
            if (processor != null) {
                processorPool.releaseProcessor(processor);
                log("Released processor back to pool");
            }
            try {
                socket.close();
            } catch (IOException e) {
                log("Error closing socket for " + socket.getInetAddress() + ": " + e.getMessage());
            }
        }
    }

    @Override
    protected void stop() {
        running = false;
        try {
            if (connector != null) {
                connector.close();
            }
        } catch (Exception e) {
            log("Error closing server socket: " + e.getMessage());
        }
        executorService.shutdown();
    }
} 