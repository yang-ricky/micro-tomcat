package com.microtomcat.jmx;

import com.microtomcat.container.Engine;
import com.microtomcat.processor.ProcessorPool;

/**
 * 标准的 MBean 实现类
 */
public class StandardServer implements StandardServerMBean {
    private final ProcessorPool processorPool;
    private final Engine engine;
    
    public StandardServer(ProcessorPool processorPool, Engine engine) {
        this.processorPool = processorPool;
        this.engine = engine;
    }

    @Override
    public String getState() {
        return engine.getState();
    }

    @Override
    public int getActiveConnections() {
        return processorPool.getCurrentLoad();
    }

    @Override
    public int getActiveThreads() {
        return processorPool.getActiveCount();
    }

    @Override
    public long getTotalRequests() {
        return processorPool.getRequestCount();
    }

    @Override
    public double getAverageResponseTime() {
        return processorPool.getAverageProcessingTime();
    }
    
    @Override
    public void reload() {
        // TODO: 实现重新加载功能
    }
    
    @Override
    public void setLogLevel(String level) {
        // TODO: 实现日志级别设置
    }
}