package com.microtomcat.cluster;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

public class ClusterRegistry {
    private final Map<String, ClusterNode> nodes = new ConcurrentHashMap<>();
    private static final ClusterRegistry instance = new ClusterRegistry();
    private ClusterNode currentNode;

    private ClusterRegistry() {}

    public static ClusterRegistry getInstance() {
        return instance;
    }

    public void registerNode(ClusterNode node) {
        if (validateNode(node)) {
            nodes.put(node.getId(), node);
            System.out.println("[ClusterRegistry] Node registered: " + node);
        }
    }

    public void unregisterNode(String nodeId) {
        ClusterNode node = nodes.remove(nodeId);
        if (node != null) {
            System.out.println("[ClusterRegistry] Node unregistered: " + node);
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
            System.out.println("[ClusterRegistry] Invalid node name");
            return false;
        }
        if (node.getHost() == null || node.getHost().trim().isEmpty()) {
            System.out.println("[ClusterRegistry] Invalid host");
            return false;
        }
        if (node.getPort() <= 0 || node.getPort() > 65535) {
            System.out.println("[ClusterRegistry] Invalid port number");
            return false;
        }
        return true;
    }

    public void setCurrentNode(ClusterNode node) {
        this.currentNode = node;
    }

    public ClusterNode getCurrentNode() {
        return currentNode;
    }

    public void updateNodeStatus(ClusterNode node, NodeStatus status) {
        node.setStatus(status);
        System.out.println("[ClusterRegistry] Node status updated: " + node + " -> " + status);
    }
} 