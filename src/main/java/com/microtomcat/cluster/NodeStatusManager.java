package com.microtomcat.cluster;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NodeStatusManager {
    private final Map<String, ClusterNode> nodes = new ConcurrentHashMap<>();
    private final Map<String, NodeStatusListener> listeners = new ConcurrentHashMap<>();

    public void updateNodeStatus(ClusterNode node, NodeStatus newStatus) {
        NodeStatus oldStatus = node.getStatus();
        if (oldStatus != newStatus) {
            node.setStatus(newStatus);
            notifyStatusChange(node, oldStatus, newStatus);
        }
    }

    public void addStatusListener(String listenerId, NodeStatusListener listener) {
        listeners.put(listenerId, listener);
    }

    public void removeStatusListener(String listenerId) {
        listeners.remove(listenerId);
    }

    private void notifyStatusChange(ClusterNode node, NodeStatus oldStatus, NodeStatus newStatus) {
        for (NodeStatusListener listener : listeners.values()) {
            if (oldStatus == NodeStatus.RUNNING && newStatus == NodeStatus.UNREACHABLE) {
                listener.onNodeDown(node);
            } else if (oldStatus == NodeStatus.UNREACHABLE && newStatus == NodeStatus.RUNNING) {
                listener.onNodeUp(node);
            }
        }
    }

    public Collection<ClusterNode> getAllNodes() {
        return nodes.values();
    }

    public void addNode(ClusterNode node) {
        nodes.put(node.getId(), node);
    }

    public void removeNode(String nodeId) {
        nodes.remove(nodeId);
    }
} 