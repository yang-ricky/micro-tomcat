package com.microtomcat.cluster;

public class LoggingNodeStatusListener implements NodeStatusListener {
    @Override
    public void onNodeDown(ClusterNode node) {
        System.out.printf("[CLUSTER] Node %s (%s:%d) is DOWN%n", 
            node.getName(), node.getHost(), node.getPort());
    }

    @Override
    public void onNodeUp(ClusterNode node) {
        System.out.printf("[CLUSTER] Node %s (%s:%d) is UP%n", 
            node.getName(), node.getHost(), node.getPort());
    }
} 