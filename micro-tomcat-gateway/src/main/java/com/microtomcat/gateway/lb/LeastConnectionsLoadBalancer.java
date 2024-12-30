package com.microtomcat.gateway.lb;

import com.microtomcat.cluster.ClusterNode;
import com.microtomcat.gateway.model.RequestWrapper;
import java.util.Comparator;
import java.util.List;

public class LeastConnectionsLoadBalancer implements LoadBalancer {

    @Override
    public ClusterNode selectNode(RequestWrapper request, List<ClusterNode> healthyNodes) {
        if (healthyNodes == null || healthyNodes.isEmpty()) {
            return null;
        }

        // 选择连接数最少的节点
        return healthyNodes.stream()
                .min(Comparator.comparingInt(ClusterNode::getConnectionCount))
                .orElse(null);
    }
} 