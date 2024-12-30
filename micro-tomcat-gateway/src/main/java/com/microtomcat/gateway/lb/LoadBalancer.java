package com.microtomcat.gateway.lb;

import com.microtomcat.cluster.ClusterNode;
import com.microtomcat.gateway.model.RequestWrapper;
import java.util.List;

public interface LoadBalancer {
    ClusterNode selectNode(RequestWrapper request, List<ClusterNode> healthyNodes);
} 