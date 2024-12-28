package com.microtomcat.server;

import com.microtomcat.connector.Connector;
import com.microtomcat.processor.Processor;
import com.microtomcat.processor.ProcessorPool;
import com.microtomcat.servlet.ServletLoader;
import com.microtomcat.session.SessionManager;
import com.microtomcat.context.Context;
import com.microtomcat.container.Engine;
import com.microtomcat.container.Host;
import com.microtomcat.lifecycle.LifecycleException;
import com.microtomcat.jmx.MBeanRegistry;
import com.microtomcat.jmx.StandardServer;
import javax.management.JMException;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlockingHttpServer extends AbstractHttpServer {
    private final ExecutorService executorService;
    private final ProcessorPool processorPool;
    private final Connector connector;
    private final Engine engine;
    private volatile boolean running = true;
    private MBeanRegistry mbeanRegistry;
    private StandardServer serverMBean;

    public BlockingHttpServer(ServerConfig config) {
        super(config);
        this.executorService = Executors.newFixedThreadPool(config.getThreadPoolSize());
        
        this.engine = new Engine("MicroTomcat", "localhost");
        
        Host defaultHost = new Host("localhost");
        engine.addChild(defaultHost);
        
        try {
            Context rootContext = new Context("", config.getWebRoot());
            defaultHost.addChild(rootContext);
            
            Context app1Context = new Context("/app1", config.getWebRoot() + "/app1");
            Context app2Context = new Context("/app2", config.getWebRoot() + "/app2");
            defaultHost.addChild(app1Context);
            defaultHost.addChild(app2Context);
            
            this.processorPool = new ProcessorPool(
                100,
                config.getWebRoot(),
                engine
            );
            
            this.connector = new Connector(config.getPort(), processorPool);
            
            engine.init();
            processorPool.init();
            connector.init();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize server", e);
        }
    }

    @Override
    public void start() {
        try {
            engine.start();
            processorPool.start();
            connector.start();
            
            for (int i = 0; i < config.getThreadPoolSize(); i++) {
                executorService.submit(new ConnectionHandler());
            }
            
            log("Server started on port " + config.getPort());
            
            synchronized (this) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (LifecycleException  e) {
            throw new RuntimeException("Failed to start server", e);
        }
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
    public void stop() {
        running = false;
        try {
            connector.stop();
            processorPool.stop();
            engine.stop();
            executorService.shutdown();
        } catch (Exception e) {
            log("Error while stopping server: " + e.getMessage());
        }
    }

    @Override
    protected void initInternal() throws LifecycleException {
        super.initInternal();
        
        try {
            System.out.println("Initializing JMX support...");
            mbeanRegistry = new MBeanRegistry();
            
            System.out.println("Registering Server MBean...");
            serverMBean = new StandardServer(processorPool, engine);
            mbeanRegistry.registerMBean(serverMBean, "StandardServer");
            
            System.out.println("Registering ProcessorPool MBean...");
            mbeanRegistry.registerMBean(processorPool, "ProcessorPool");
            
            System.out.println("Registering Connector MBean...");
            mbeanRegistry.registerMBean(connector, "Connector");
            
            System.out.println("JMX initialization completed.");
        } catch (JMException e) {
            throw new LifecycleException("Failed to initialize JMX support", e);
        }
    }
} 