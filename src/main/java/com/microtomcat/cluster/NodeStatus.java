package com.microtomcat.cluster;

public enum NodeStatus {
    NEW,        // 节点刚创建
    STARTING,   // 节点正在启动
    RUNNING,    // 节点正常运行
    UNREACHABLE,// 节点不可达
    STOPPED,    // 节点已停止
    FAILED      // 节点故障
} 