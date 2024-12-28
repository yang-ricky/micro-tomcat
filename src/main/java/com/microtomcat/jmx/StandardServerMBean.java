package com.microtomcat.jmx;

/**
 * 标准的 MBean 接口命名必须是实现类名 + MBean 后缀
 */
public interface StandardServerMBean {
    // 服务器状态
    String getState();
    
    // 性能指标
    int getActiveConnections();
    int getActiveThreads();
    long getTotalRequests();
    double getAverageResponseTime();
    
    // 操作方法
    void reload();
    void setLogLevel(String level);
} 