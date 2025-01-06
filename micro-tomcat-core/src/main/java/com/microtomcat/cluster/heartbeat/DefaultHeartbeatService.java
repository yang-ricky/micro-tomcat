package com.microtomcat.cluster.heartbeat;

import com.microtomcat.cluster.ClusterNode;
import com.microtomcat.cluster.ClusterRegistry;
import com.microtomcat.cluster.NodeStatus;
import com.microtomcat.cluster.NodeStatusManager;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.net.Socket;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;

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
        try {
            // 创建到目标节点的 Socket 连接
            Socket socket = new Socket(node.getHost(), node.getPort());
            
            // 发送 HTTP GET 请求
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("GET /ping HTTP/1.1");
            out.println("Host: " + node.getHost() + ":" + node.getPort());
            out.println("Connection: close");
            out.println(); // 空行表示请求头结束
            
            // 读取响应
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String responseLine = in.readLine();
            
            // 关闭连接
            socket.close();
            
            // 检查响应状态码是否为 200
            return responseLine != null && responseLine.contains("200 OK");
            
        } catch (Exception e) {
            logger.warning("[HeartbeatService] Failed to ping node " + node.getId() + ": " + e.getMessage());
            return false;
        }
    }

    private boolean isCurrentNode(ClusterNode node) {
        ClusterNode currentNode = clusterRegistry.getCurrentNode();
        return currentNode != null && 
               currentNode.getPort() == node.getPort() && 
               currentNode.getHost().equals(node.getHost());
    }
} 