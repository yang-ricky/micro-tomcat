package com.microtomcat.processor;

import com.microtomcat.servlet.ServletLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import com.microtomcat.session.SessionManager;
import com.microtomcat.context.ContextManager;
import java.io.IOException;
import com.microtomcat.lifecycle.LifecycleBase;
import com.microtomcat.lifecycle.LifecycleException;

public class ProcessorPool extends LifecycleBase {
    private final BlockingQueue<Processor> pool;
    private final List<Processor> allProcessors;
    private final int maxProcessors;
    private final String webRoot;
    private final ContextManager contextManager;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();
    private final AtomicInteger activeProcessors = new AtomicInteger(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    private final AtomicLong requestCount = new AtomicLong(0);

    public ProcessorPool(int maxProcessors, String webRoot, ContextManager contextManager) {
        this.maxProcessors = maxProcessors;
        this.webRoot = webRoot;
        this.contextManager = contextManager;
        this.pool = new ArrayBlockingQueue<>(maxProcessors);
        this.allProcessors = new ArrayList<>();
        
        // 预创建一些处理器
        int initialProcessors = Math.min(maxProcessors / 2, 10);
        for (int i = 0; i < initialProcessors; i++) {
            createProcessor(new SessionManager());
        }
    }

    @Override
    protected void initInternal() throws LifecycleException {
        log("Initializing ProcessorPool with " + allProcessors.size() + " processors");
        for (Processor processor : allProcessors) {
            processor.init();
        }
    }

    @Override
    protected void startInternal() throws LifecycleException {
        log("Starting ProcessorPool");
        for (Processor processor : allProcessors) {
            processor.start();
        }
    }

    @Override
    protected void stopInternal() throws LifecycleException {
        log("Stopping ProcessorPool");
        for (Processor processor : allProcessors) {
            processor.stop();
        }
    }

    @Override
    protected void destroyInternal() throws LifecycleException {
        log("Destroying ProcessorPool");
        for (Processor processor : allProcessors) {
            processor.destroy();
        }
        pool.clear();
        allProcessors.clear();
    }

    private void log(String message) {
        System.out.println("[ProcessorPool] " + message);
    }

    public Processor getProcessor(long timeout) throws InterruptedException {
        lock.lock();
        try {
            long nanos = TimeUnit.MILLISECONDS.toNanos(timeout);
            while (pool.isEmpty() && nanos > 0) {
                nanos = notEmpty.awaitNanos(nanos);
            }
            Processor processor = pool.poll();
            if (processor != null) {
                notFull.signal();
            }
            return processor;
        } finally {
            lock.unlock();
        }
    }

    public void releaseProcessor(Processor processor) {
        lock.lock();
        try {
            pool.offer(processor);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    private void createProcessor(SessionManager sessionManager) {
        try {
            ServletLoader servletLoader = new ServletLoader(webRoot, webRoot + "/WEB-INF/classes");
            Processor processor = new Processor(webRoot, servletLoader, sessionManager, contextManager);
            allProcessors.add(processor);
            pool.offer(processor);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create processor", e);
        }
    }

    public int getActiveCount() {
        return allProcessors.size() - pool.size();
    }

    public int getTotalCount() {
        return allProcessors.size();
    }

    public void recordProcessingTime(long processingTimeMs) {
        totalProcessingTime.addAndGet(processingTimeMs);
        requestCount.incrementAndGet();
    }

    public double getAverageProcessingTime() {
        long count = requestCount.get();
        return count > 0 ? (double) totalProcessingTime.get() / count : 0;
    }

    public int getCurrentLoad() {
        return activeProcessors.get();
    }
} 