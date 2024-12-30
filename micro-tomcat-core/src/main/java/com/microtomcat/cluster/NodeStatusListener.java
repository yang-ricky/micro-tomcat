package com.microtomcat.cluster;

public interface NodeStatusListener {
    void onNodeDown(ClusterNode node);
    void onNodeUp(ClusterNode node);
} 