package com.microtomcat.cluster.config;

import java.util.List;
import java.util.ArrayList;

public class ClusterConfig {
    private String clusterName;
    private List<NodeConfig> nodes;
    private long heartbeatInterval = 5000; // 默认5秒
    private long heartbeatTimeout = 3000;  // 默认3秒

    public ClusterConfig() {
        this.nodes = new ArrayList<>();
    }

    // Getters and Setters
    public String getClusterName() { return clusterName; }
    public void setClusterName(String clusterName) { this.clusterName = clusterName; }
    public List<NodeConfig> getNodes() { return nodes; }
    public void setNodes(List<NodeConfig> nodes) { this.nodes = nodes; }
    public long getHeartbeatInterval() {
        return heartbeatInterval;
    }
    public void setHeartbeatInterval(long heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }
    public long getHeartbeatTimeout() {
        return heartbeatTimeout;
    }
    public void setHeartbeatTimeout(long heartbeatTimeout) {
        this.heartbeatTimeout = heartbeatTimeout;
    }

    public static class NodeConfig {
        private String name;
        private String host;
        private int port;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
    }
} 