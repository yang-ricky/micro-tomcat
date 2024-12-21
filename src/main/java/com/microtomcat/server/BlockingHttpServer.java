package com.microtomcat.server;

import com.microtomcat.processor.Processor;
import com.microtomcat.processor.ProcessorPool;
import com.microtomcat.servlet.ServletLoader;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlockingHttpServer extends AbstractHttpServer {
    private final ExecutorService executorService;
    private final ServletLoader servletLoader;
    private final ProcessorPool processorPool;
    private ServerSocket serverSocket;
    private volatile boolean running = true;

    public BlockingHttpServer(ServerConfig config) {
        super(config);
        this.executorService = Executors.newFixedThreadPool(config.getThreadPoolSize());
        try {
            this.servletLoader = new ServletLoader(config.getWebRoot(), "target/classes");
            this.processorPool = new ProcessorPool(100, config.getWebRoot(), servletLoader);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize server", e);
        }
    }

    @Override
    public void start() throws IOException {
        serverSocket = new ServerSocket(config.getPort());
        log("Blocking server started on port: " + config.getPort());

        while (running) {
            try {
                Socket socket = serverSocket.accept();
                log("New connection accepted from: " + socket.getInetAddress());
                executorService.execute(() -> handleRequest(socket));
            } catch (IOException e) {
                if (running) {
                    log("Error accepting connection: " + e.getMessage());
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
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log("Error closing server socket: " + e.getMessage());
        }
        executorService.shutdown();
    }
} 