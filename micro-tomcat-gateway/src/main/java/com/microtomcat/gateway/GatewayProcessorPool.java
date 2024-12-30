package com.microtomcat.gateway;

import com.microtomcat.cluster.ClusterRegistry;
import com.microtomcat.gateway.lb.LoadBalancer;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class GatewayProcessorPool {
    
    private final int maxProcessors;
    private final LoadBalancer loadBalancer;
    private final ClusterRegistry clusterRegistry;
    private final ThreadPoolExecutor executorService;
    private final BlockingQueue<BalancingProcessor> pool;
    private final AtomicInteger currentConnections = new AtomicInteger(0);
    private volatile boolean running = false;
    
    public GatewayProcessorPool(int maxProcessors, LoadBalancer loadBalancer, ClusterRegistry clusterRegistry) {
        this.maxProcessors = maxProcessors;
        this.loadBalancer = loadBalancer;
        this.clusterRegistry = clusterRegistry;
        this.pool = new ArrayBlockingQueue<>(maxProcessors);
        
        // 创建线程池
        this.executorService = new ThreadPoolExecutor(
            maxProcessors / 2,  // 核心线程数
            maxProcessors,      // 最大线程数
            60L,               // 空闲线程存活时间
            TimeUnit.SECONDS,  
            new ArrayBlockingQueue<>(100),
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(0);
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName("gateway-processor-" + counter.incrementAndGet());
                    return thread;
                }
            }
        );
        
        // 预创建处理器
        for (int i = 0; i < maxProcessors / 2; i++) {
            pool.offer(createProcessor());
        }
    }
    
    public void start() {
        running = true;
    }
    
    public void stop() {
        running = false;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    public void process(Socket socket) {
        if (!running) {
            try {
                socket.close();
            } catch (IOException e) {
            }
            return;
        }
        
        currentConnections.incrementAndGet();
        
        executorService.execute(() -> {
            BalancingProcessor processor = null;
            try {
                processor = getProcessor();
                if (processor != null) {
                    processor.process(socket);
                }
            } catch (Exception e) {
            } finally {
                if (processor != null) {
                    returnProcessor(processor);
                }
                currentConnections.decrementAndGet();
            }
        });
    }
    
    private BalancingProcessor createProcessor() {
        return new BalancingProcessor(loadBalancer, clusterRegistry);
    }
    
    private BalancingProcessor getProcessor() {
        BalancingProcessor processor = pool.poll();
        if (processor == null) {
            processor = createProcessor();
        }
        return processor;
    }
    
    private void returnProcessor(BalancingProcessor processor) {
        if (running) {
            pool.offer(processor);
        }
    }
    
    public int getCurrentConnections() {
        return currentConnections.get();
    }
} 