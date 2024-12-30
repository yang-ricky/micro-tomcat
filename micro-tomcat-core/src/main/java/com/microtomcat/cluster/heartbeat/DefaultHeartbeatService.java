package com.microtomcat.cluster.heartbeat;

import com.microtomcat.cluster.ClusterNode;
import com.microtomcat.cluster.NodeStatus;
import com.microtomcat.cluster.NodeStatusManager;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DefaultHeartbeatService implements HeartbeatService {
    private final ScheduledExecutorService scheduler;
    private final NodeStatusManager statusManager;
    private long heartbeatInterval = 5000; // 默认5秒
    private long heartbeatTimeout = 3000;  // 默认3秒超时
    private volatile boolean running = false;

    public DefaultHeartbeatService(NodeStatusManager statusManager) {
        this.statusManager = statusManager;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void start() {
        running = true;
        scheduler.scheduleAtFixedRate(this::heartbeat, 0, heartbeatInterval, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        running = false;
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[HeartbeatService] Interrupted while stopping");
        }
    }

    private void heartbeat() {
        if (!running) return;
        
        for (ClusterNode node : statusManager.getAllNodes()) {
            checkNode(node);
        }
    }

    @Override
    public void checkNode(ClusterNode node) {
        try {
            String url = String.format("http://%s:%d/ping", node.getHost(), node.getPort());
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout((int) heartbeatTimeout);
            conn.setReadTimeout((int) heartbeatTimeout);
            conn.setRequestMethod("GET");
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                statusManager.updateNodeStatus(node, NodeStatus.RUNNING);
            } else {
                statusManager.updateNodeStatus(node, NodeStatus.UNREACHABLE);
            }
        } catch (IOException e) {
            statusManager.updateNodeStatus(node, NodeStatus.UNREACHABLE);
        }
    }

    @Override
    public void setHeartbeatInterval(long interval) {
        this.heartbeatInterval = interval;
    }

    @Override
    public void setHeartbeatTimeout(long timeout) {
        this.heartbeatTimeout = timeout;
    }
} 