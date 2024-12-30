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
import com.microtomcat.context.Context;
import com.microtomcat.context.ContextManager;
import com.microtomcat.processor.ProcessorPoolMBean;

import java.io.IOException;
import com.microtomcat.lifecycle.LifecycleBase;
import com.microtomcat.lifecycle.LifecycleException;
import com.microtomcat.container.Container;
import com.microtomcat.container.Engine;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

public class ProcessorPool extends LifecycleBase implements ProcessorPoolMBean {
    private final BlockingQueue<Processor> pool;
    private final List<Processor> allProcessors;
    private final int maxProcessors;
    private final String webRoot;
    private final Engine engine;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();
    private final AtomicInteger activeProcessors = new AtomicInteger(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    private final AtomicLong requestCount = new AtomicLong(0);
    private final AtomicInteger currentLoad = new AtomicInteger(0);
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final ExecutorService executorService;

    public ProcessorPool(int maxProcessors, String webRoot, Engine engine) {
        this.maxProcessors = maxProcessors;
        this.webRoot = webRoot;
        this.engine = engine;
        this.pool = new ArrayBlockingQueue<>(maxProcessors);
        this.allProcessors = new ArrayList<>();
        
        // 预创建处理器
        int initialProcessors = Math.min(maxProcessors / 2, 10);
        for (int i = 0; i < initialProcessors; i++) {
            createProcessor();
        }
        
        this.executorService = new ThreadPoolExecutor(
            1,                      // 核心线程数
            maxProcessors,            // 最大线程数
            60L,                   // 空闲线程存活时间
            TimeUnit.SECONDS,      // 时间单位
            new ArrayBlockingQueue<>(100)  // 工作队列
        );
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

    private void createProcessor() {
        Processor processor = new Processor(webRoot, engine);
        allProcessors.add(processor);
        pool.offer(processor);
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

    @Override
    public double getAverageProcessingTime() {
        long count = requestCount.get();
        return count > 0 ? (double) totalProcessingTime.get() / count : 0;
    }

    public int getCurrentLoad() {
        return currentLoad.get();
    }

    public void recordRequest(long processingTimeMs) {
        totalRequests.incrementAndGet();
        totalProcessingTime.addAndGet(processingTimeMs);
    }

    public long getRequestCount() {
        return totalRequests.get();
    }

    protected void incrementLoad() {
        currentLoad.incrementAndGet();
    }

    protected void decrementLoad() {
        currentLoad.decrementAndGet();
    }

    @Override
    public int getActiveProcessors() {
        return getActiveCount();
    }
    
    @Override
    public int getIdleProcessors() {
        return pool.size();
    }
    
    @Override
    public long getTotalProcessedRequests() {
        return getRequestCount();
    }
    
    @Override
    public void setMaxProcessors(int max) {
        // 动态调整处理器池大小
        lock.lock();
        try {
            while (allProcessors.size() < max) {
                createProcessor();
            }
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public void invalidateAllSessions() {
        // 遍历所有 Context 并使其 Session 失效
        for (Container child : engine.findChildren()) {
            if (child instanceof Context) {
                Context context = (Context) child;
                context.getSessionManager().invalidateAll();
            }
        }
    }

    public ExecutorService getExecutor() {
        return executorService;
    }
} 