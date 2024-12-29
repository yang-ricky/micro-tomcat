package com.microtomcat.cluster.config;

import java.util.List;
import java.util.ArrayList;

public class ClusterConfig {
    private String clusterName;
    private List<NodeConfig> nodes;
    private int heartbeatInterval;
    private int heartbeatTimeout;

    public ClusterConfig() {
        this.nodes = new ArrayList<>();
        this.heartbeatInterval = 5000; // 默认5秒
        this.heartbeatTimeout = 15000; // 默认15秒
    }

    // Getters and Setters
    public String getClusterName() { return clusterName; }
    public void setClusterName(String clusterName) { this.clusterName = clusterName; }
    public List<NodeConfig> getNodes() { return nodes; }
    public void setNodes(List<NodeConfig> nodes) { this.nodes = nodes; }
    public int getHeartbeatInterval() { return heartbeatInterval; }
    public void setHeartbeatInterval(int heartbeatInterval) { this.heartbeatInterval = heartbeatInterval; }
    public int getHeartbeatTimeout() { return heartbeatTimeout; }
    public void setHeartbeatTimeout(int heartbeatTimeout) { this.heartbeatTimeout = heartbeatTimeout; }

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