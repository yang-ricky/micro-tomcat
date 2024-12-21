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

public class ProcessorPool {
    private final BlockingQueue<Processor> pool;
    private final List<Processor> allProcessors;
    private final int maxProcessors;
    private final String webRoot;
    private final ServletLoader servletLoader;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();
    private final AtomicInteger activeProcessors = new AtomicInteger(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    private final AtomicLong requestCount = new AtomicLong(0);

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