package com.microtomcat.gateway.lb;

import com.microtomcat.cluster.ClusterNode;
import com.microtomcat.gateway.model.RequestWrapper;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinLoadBalancer implements LoadBalancer {
    private final AtomicInteger counter = new AtomicInteger(0);
    
    @Override
    public ClusterNode selectNode(RequestWrapper request, List<ClusterNode> healthyNodes) {
        if (healthyNodes == null || healthyNodes.isEmpty()) {
            return null;
        }
        int index = counter.incrementAndGet() % healthyNodes.size();
        return healthyNodes.get(index);
    }
} 