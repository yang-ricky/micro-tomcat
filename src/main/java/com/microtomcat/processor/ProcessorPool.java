package com.microtomcat.processor;

import com.microtomcat.servlet.ServletLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class ProcessorPool {
    private final BlockingQueue<Processor> pool;
    private final List<Processor> allProcessors;
    private final int maxProcessors;
    private final String webRoot;
    private final ServletLoader servletLoader;

    public ProcessorPool(int maxProcessors, String webRoot, ServletLoader servletLoader) {
        this.maxProcessors = maxProcessors;
        this.webRoot = webRoot;
        this.servletLoader = servletLoader;
        this.pool = new ArrayBlockingQueue<>(maxProcessors);
        this.allProcessors = new ArrayList<>();
        
        // 预创建一些处理器
        int initialProcessors = Math.min(maxProcessors / 2, 10);
        for (int i = 0; i < initialProcessors; i++) {
            createProcessor();
        }
    }

    public Processor getProcessor(long timeout) throws InterruptedException {
        Processor processor = pool.poll(timeout, TimeUnit.MILLISECONDS);
        if (processor == null && allProcessors.size() < maxProcessors) {
            synchronized (this) {
                if (allProcessors.size() < maxProcessors) {
                    processor = createProcessor();
                }
            }
        }
        if (processor != null) {
            processor.setAvailable(false);
        }
        return processor;
    }

    public void releaseProcessor(Processor processor) {
        if (processor != null) {
            processor.setAvailable(true);
            pool.offer(processor);
        }
    }

    private Processor createProcessor() {
        Processor processor = new Processor(webRoot, servletLoader);
        pool.offer(processor);
        allProcessors.add(processor);
        return processor;
    }

    public int getActiveCount() {
        return allProcessors.size() - pool.size();
    }

    public int getTotalCount() {
        return allProcessors.size();
    }
} 