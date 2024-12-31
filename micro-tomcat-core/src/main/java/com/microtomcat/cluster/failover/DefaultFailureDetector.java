package com.microtomcat.cluster.failover;

import com.microtomcat.cluster.ClusterNode;
import com.microtomcat.cluster.ClusterRegistry;
import com.microtomcat.cluster.NodeStatus;
import java.util.logging.Logger;

public class DefaultFailureDetector implements FailureDetector {
    private static final Logger logger = Logger.getLogger(DefaultFailureDetector.class.getName());
    private final ClusterRegistry clusterRegistry;

    public DefaultFailureDetector(ClusterRegistry clusterRegistry) {
        this.clusterRegistry = clusterRegistry;
    }

    @Override
    public void onNodeStatusChange(ClusterNode node, NodeStatus oldStatus, NodeStatus newStatus) {
        logger.info(String.format("[FailureDetector] Node %s status changed from %s to %s", 
            node.getId(), oldStatus, newStatus));
            
        if (oldStatus == NodeStatus.RUNNING && newStatus == NodeStatus.UNREACHABLE) {
            handleNodeFailure(node);
        } else if (oldStatus == NodeStatus.UNREACHABLE && newStatus == NodeStatus.RUNNING) {
            handleNodeRecovery(node);
        }
    }

    @Override
    public void handleNodeFailure(ClusterNode node) {
        logger.warning(String.format("[FailureDetector] Node %s is down, cleaning up resources", node.getId()));
        // 清理节点相关资源
        cleanupNodeResources(node);
        // 更新集群注册表中的节点状态
        clusterRegistry.updateNodeStatus(node, NodeStatus.UNREACHABLE);
    }

    @Override
    public void handleNodeRecovery(ClusterNode node) {
        logger.info(String.format("[FailureDetector] Node %s is recovering", node.getId()));
        // 更新集群注册表中的节点状态
        clusterRegistry.updateNodeStatus(node, NodeStatus.RUNNING);
        // 重新分配资源
        reallocateResources(node);
    }

    private void cleanupNodeResources(ClusterNode node) {
        // 在这里实现资源清理逻辑，比如：
        // 1. 关闭与该节点的连接
        // 2. 清理该节点的缓存
        // 3. 释放该节点占用的内存等
        logger.info(String.format("[FailureDetector] Cleaned up resources for node %s", node.getId()));
    }

    private void reallocateResources(ClusterNode node) {
        // 在这里实现资源重新分配逻辑，比如：
        // 1. 重新建立连接
        // 2. 同步必要的数据
        // 3. 重新分配内存等
        logger.info(String.format("[FailureDetector] Reallocated resources for node %s", node.getId()));
    }
} 