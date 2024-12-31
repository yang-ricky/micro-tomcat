package com.microtomcat.cluster;

import com.microtomcat.cluster.failover.FailureDetector;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class NodeStatusManager {
    private static final Logger logger = Logger.getLogger(NodeStatusManager.class.getName());
    private final Map<String, NodeStatusListener> listeners = new ConcurrentHashMap<>();
    private final FailureDetector failureDetector;
    private final ClusterRegistry clusterRegistry;

    public NodeStatusManager(ClusterRegistry clusterRegistry, FailureDetector failureDetector) {
        this.clusterRegistry = clusterRegistry;
        this.failureDetector = failureDetector;
    }

    public void updateNodeStatus(ClusterNode node, NodeStatus newStatus) {
        NodeStatus oldStatus = node.getStatus();
        if (oldStatus != newStatus) {
            logger.info(String.format("[NodeStatusManager] Node %s status changing from %s to %s", 
                node.getId(), oldStatus, newStatus));
            
            // 更新节点状态
            node.setStatus(newStatus);
            
            // 通知故障检测器
            failureDetector.onNodeStatusChange(node, oldStatus, newStatus);
            
            // 通知其他监听器
            notifyListeners(node, oldStatus, newStatus);
        }
    }

    private void notifyListeners(ClusterNode node, NodeStatus oldStatus, NodeStatus newStatus) {
        for (NodeStatusListener listener : listeners.values()) {
            try {
                if (newStatus == NodeStatus.UNREACHABLE) {
                    listener.onNodeDown(node);
                } else if (newStatus == NodeStatus.RUNNING) {
                    listener.onNodeUp(node);
                }
            } catch (Exception e) {
                logger.severe(String.format("[NodeStatusManager] Error notifying listener for node %s: %s", 
                    node.getId(), e.getMessage()));
            }
        }
    }

    public void addStatusListener(String id, NodeStatusListener listener) {
        listeners.put(id, listener);
    }

    public void removeStatusListener(String id) {
        listeners.remove(id);
    }
}