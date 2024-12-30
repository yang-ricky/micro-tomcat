package com.microtomcat.cluster;

import java.util.UUID;

public class ClusterNode {
    private final String id;
    private final String name;
    private final String host;
    private final int port;
    private NodeStatus status;
    private long lastHeartbeat;

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

    @Override
    public String toString() {
        return String.format("ClusterNode[name=%s, host=%s, port=%d, status=%s]", 
            name, host, port, status);
    }
}