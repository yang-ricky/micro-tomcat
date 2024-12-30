package com.microtomcat.cluster;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ClusterNode {
    private final String id;
    private String name;
    private String host;
    private int port;
    private NodeStatus status;
    private long lastHeartbeat;
    private final AtomicInteger connectionCount = new AtomicInteger(0);

    public ClusterNode(String name, String host, int port) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.host = host;
        this.port = port;
        this.status = NodeStatus.STARTING;
        this.lastHeartbeat = System.currentTimeMillis();
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public NodeStatus getStatus() { return status; }
    public long getLastHeartbeat() { return lastHeartbeat; }

    // Setters
    public void setStatus(NodeStatus status) { 
        this.status = status; 
    }
    public void updateHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
    }

    // 获取当前连接数
    public int getConnectionCount() {
        return connectionCount.get();
    }

    // 增加连接数
    public void incrementConnectionCount() {
        connectionCount.incrementAndGet();
    }

    // 减少连接数
    public void decrementConnectionCount() {
        connectionCount.decrementAndGet();
    }

    @Override
    public String toString() {
        return String.format("ClusterNode[name=%s, host=%s, port=%d, status=%s, connections=%d]", 
            name, host, port, status, connectionCount.get());
    }
}