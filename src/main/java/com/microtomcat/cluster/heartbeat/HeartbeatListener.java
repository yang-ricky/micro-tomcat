package com.microtomcat.cluster.heartbeat;

public interface HeartbeatListener {
    void onHeartbeat(String nodeId, boolean isAlive);
} 