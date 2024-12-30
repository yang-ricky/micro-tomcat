package com.microtomcat.gateway.lb;

import com.microtomcat.cluster.ClusterNode;
import com.microtomcat.gateway.model.RequestWrapper;
import java.util.List;

public class HashLoadBalancer implements LoadBalancer {
    
    @Override
    public ClusterNode selectNode(RequestWrapper request, List<ClusterNode> healthyNodes) {
        if (healthyNodes == null || healthyNodes.isEmpty()) {
            return null;
        }
        
        // 获取 JSESSIONID
        String sessionId = request.getSessionId();
        if (sessionId == null) {
            // 如果没有session，fallback到简单轮询
            return healthyNodes.get((int)(System.nanoTime() % healthyNodes.size()));
        }
        
        // 使用 session id 的 hashCode
        int hash = sessionId.hashCode();
        return healthyNodes.get(Math.abs(hash % healthyNodes.size()));
    }
} 