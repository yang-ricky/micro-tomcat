package com.microtomcat.cluster;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

public class ClusterRegistry {
    private final Map<String, ClusterNode> nodes = new ConcurrentHashMap<>();
    private static final ClusterRegistry instance = new ClusterRegistry();

    private ClusterRegistry() {}

    public static ClusterRegistry getInstance() {
        return instance;
    }

    public void registerNode(ClusterNode node) {
        if (validateNode(node)) {
            nodes.put(node.getId(), node);
            log("Node registered: " + node);
        }
    }

    public void unregisterNode(String nodeId) {
        ClusterNode node = nodes.remove(nodeId);
        if (node != null) {
            log("Node unregistered: " + node);
        }
    }

    public List<ClusterNode> getAllNodes() {
        return new ArrayList<>(nodes.values());
    }

    public ClusterNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    private boolean validateNode(ClusterNode node) {
        if (node.getName() == null || node.getName().trim().isEmpty()) {
            log("Invalid node name");
            return false;
        }
        if (node.getHost() == null || node.getHost().trim().isEmpty()) {
            log("Invalid host");
            return false;
        }
        if (node.getPort() <= 0 || node.getPort() > 65535) {
            log("Invalid port number");
            return false;
        }
        return true;
    }

    private void log(String message) {
        System.out.println("[ClusterRegistry] " + message);
    }
} 