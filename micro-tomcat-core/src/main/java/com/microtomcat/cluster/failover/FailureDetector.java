package com.microtomcat.cluster.failover;

import com.microtomcat.cluster.ClusterNode;
import com.microtomcat.cluster.NodeStatus;

public interface FailureDetector {
    void onNodeStatusChange(ClusterNode node, NodeStatus oldStatus, NodeStatus newStatus);
    void handleNodeFailure(ClusterNode node);
    void handleNodeRecovery(ClusterNode node);
} 