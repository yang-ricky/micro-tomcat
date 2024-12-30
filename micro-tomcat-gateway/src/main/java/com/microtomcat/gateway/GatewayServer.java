package com.microtomcat.gateway;

import com.microtomcat.cluster.ClusterRegistry;
import com.microtomcat.gateway.lb.LoadBalancer;
import com.microtomcat.gateway.lb.RoundRobinLoadBalancer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class GatewayServer {
    
    private final int port;
    private final GatewayProcessorPool processorPool;
    private final LoadBalancer loadBalancer;
    private final ClusterRegistry clusterRegistry;
    private ServerSocket serverSocket;
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    public GatewayServer(int port) {
        this.port = port;
        this.loadBalancer = new RoundRobinLoadBalancer();
        this.clusterRegistry = ClusterRegistry.getInstance();
        this.processorPool = new GatewayProcessorPool(10, loadBalancer, clusterRegistry);
    }
    
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running.set(true);
        processorPool.start();
        
        
        // 接受连接的主循环
        while (running.get()) {
            try {
                Socket socket = serverSocket.accept();
                processorPool.process(socket);
            } catch (IOException e) {
                if (running.get()) {
                }
            }
        }
    }
    
    public void stop() {
        running.set(false);
        processorPool.stop();
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {

        }
    }
    
    public static void main(String[] args) {
        int port = 8090; // 默认端口
        
        // 解析命令行参数
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--port") && i + 1 < args.length) {
                port = Integer.parseInt(args[i + 1]);
                break;
            }
        }
        
        GatewayServer server = new GatewayServer(port);
        try {
            server.start();
        } catch (IOException e) {
            System.exit(1);
        }
        
        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }
} 