package com.microtomcat.connector;

/**
 * Connector的JMX管理接口
 * 提供连接器的监控和管理功能
 */
public interface ConnectorMBean {
    /**
     * 获取连接器运行状态
     */
    boolean isRunning();
    
    /**
     * 获取当前活动线程数
     */
    int getCurrentThreadCount();
    
    /**
     * 获取当前活动连接数
     */
    int getActiveConnections();
    
    /**
     * 获取已处理的总请求数
     */
    long getTotalRequests();
    
    /**
     * 获取请求的平均响应时间（毫秒）
     */
    double getAverageResponseTime();
    
    /**
     * 设置工作线程池的最大线程数
     * @param maxThreads 最大线程数
     */
    void setMaxThreads(int maxThreads);
    
    /**
     * 设置连接超时时间
     * @param timeout 超时时间（毫秒）
     */
    void setConnectionTimeout(int timeout);
} 