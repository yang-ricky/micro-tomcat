package com.microtomcat.cluster.heartbeat;

import com.microtomcat.cluster.ClusterNode;

public interface HeartbeatService {
    void start();
    void stop();
    void checkNode(ClusterNode node);
    void setHeartbeatInterval(long interval);
    void setHeartbeatTimeout(long timeout);
} 