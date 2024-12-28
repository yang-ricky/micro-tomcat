package com.microtomcat.processor;

/**
 * ProcessorPool的JMX管理接口
 */
public interface ProcessorPoolMBean {
    // 处理器池状态
    int getActiveProcessors();
    int getIdleProcessors();
    
    // 性能指标
    long getTotalProcessedRequests();
    double getAverageProcessingTime();
    
    // 操作
    void setMaxProcessors(int max);
    void invalidateAllSessions();
} 