package com.microtomcat.cluster.heartbeat;

import com.microtomcat.cluster.ClusterNode;
import com.microtomcat.cluster.ClusterRegistry;
import com.microtomcat.cluster.NodeStatus;
import com.microtomcat.cluster.NodeStatusManager;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class DefaultHeartbeatService implements HeartbeatService {
    private static final Logger logger = Logger.getLogger(DefaultHeartbeatService.class.getName());
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final NodeStatusManager statusManager;
    private final ClusterRegistry clusterRegistry;
    private long heartbeatInterval;
    private long heartbeatTimeout;

    public DefaultHeartbeatService(NodeStatusManager statusManager, ClusterRegistry clusterRegistry, 
                                 long heartbeatInterval, long heartbeatTimeout) {
        this.statusManager = statusManager;
        this.clusterRegistry = clusterRegistry;
        this.heartbeatInterval = heartbeatInterval;
        this.heartbeatTimeout = heartbeatTimeout;
    }

    @Override
    public void setHeartbeatInterval(long interval) {
        this.heartbeatInterval = interval;
        logger.info("[HeartbeatService] Heartbeat interval updated to: " + interval + "ms");
        restart();
    }

    @Override
    public void setHeartbeatTimeout(long timeout) {
        this.heartbeatTimeout = timeout;
        logger.info("[HeartbeatService] Heartbeat timeout updated to: " + timeout + "ms");
    }

    @Override
    public void checkNode(ClusterNode node) {
        if (!isCurrentNode(node)) {
            checkNodeHealth(node);
        }
    }

    private void restart() {
        stop();
        start();
    }

    @Override
    public void start() {
        scheduler.scheduleAtFixedRate(this::checkHeartbeats, 
            heartbeatInterval, heartbeatInterval, TimeUnit.MILLISECONDS);
        logger.info("[HeartbeatService] Started with interval: " + heartbeatInterval + "ms");
    }

    @Override
    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(heartbeatTimeout, TimeUnit.MILLISECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("[HeartbeatService] Stopped");
    }

    private void checkHeartbeats() {
        for (ClusterNode node : clusterRegistry.getAllNodes()) {
            if (!isCurrentNode(node)) {
                checkNodeHealth(node);
            }
        }
    }

    private void checkNodeHealth(ClusterNode node) {
        try {
            if (ping(node)) {
                if (node.getStatus() == NodeStatus.UNREACHABLE) {
                    statusManager.updateNodeStatus(node, NodeStatus.RUNNING);
                }
            } else {
                if (node.getStatus() == NodeStatus.RUNNING) {
                    statusManager.updateNodeStatus(node, NodeStatus.UNREACHABLE);
                }
            }
        } catch (Exception e) {
            logger.warning("[HeartbeatService] Error checking node " + node.getId() + ": " + e.getMessage());
            if (node.getStatus() == NodeStatus.RUNNING) {
                statusManager.updateNodeStatus(node, NodeStatus.UNREACHABLE);
            }
        }
    }

    private boolean ping(ClusterNode node) {
        // 实现ping逻辑，例如HTTP请求node的/ping接口
        // 这里简单返回true，实际实现需要真正去ping目标节点
        return true;
    }

    private boolean isCurrentNode(ClusterNode node) {
        ClusterNode currentNode = clusterRegistry.getCurrentNode();
        return currentNode != null && 
               currentNode.getPort() == node.getPort() && 
               currentNode.getHost().equals(node.getHost());
    }
} 