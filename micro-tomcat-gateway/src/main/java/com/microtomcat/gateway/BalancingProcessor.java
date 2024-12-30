package com.microtomcat.gateway;

import com.microtomcat.cluster.ClusterNode;
import com.microtomcat.cluster.ClusterRegistry;
import com.microtomcat.cluster.NodeStatus;
import com.microtomcat.gateway.lb.LoadBalancer;
import com.microtomcat.gateway.model.RequestWrapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

public class BalancingProcessor {
    
    private final LoadBalancer loadBalancer;
    private final ClusterRegistry clusterRegistry;
    private static final int MAX_RETRIES = 3;
    
    public BalancingProcessor(LoadBalancer loadBalancer, ClusterRegistry clusterRegistry) {
        this.loadBalancer = loadBalancer;
        this.clusterRegistry = clusterRegistry;
    }
    
    public void process(Socket socket) throws IOException {
        try (InputStream input = socket.getInputStream();
             OutputStream output = socket.getOutputStream()) {
            
            // 1. 解析请求
            RequestWrapper request = parseRequest(input);
            if (request == null) {
                sendError(output, 400, "Bad Request");
                return;
            }
            
            // 2. 获取健康节点列表
            List<ClusterNode> healthyNodes = clusterRegistry.getAllNodes().stream()
                .filter(node -> node.getStatus() == NodeStatus.RUNNING)
                .collect(Collectors.toList());
                
            if (healthyNodes.isEmpty()) {
                sendError(output, 503, "No available backend servers");
                return;
            }
            
            // 3. 选择节点并转发请求
            boolean success = false;
            Exception lastError = null;
            
            for (int i = 0; i < MAX_RETRIES && !success; i++) {
                ClusterNode node = loadBalancer.selectNode(request, healthyNodes);
                if (node == null) {
                    break;
                }
                
                try {
                    forwardRequest(request, node, output);
                    success = true;
                } catch (IOException e) {
                    lastError = e;
                    // 从健康节点列表中移除失败的节点
                    healthyNodes.remove(node);
                }
            }
            
            if (!success) {
                sendError(output, 502, "Bad Gateway");
            }
            
        } catch (Exception e) {
            try {
                sendError(socket.getOutputStream(), 500, "Internal Server Error");
            } catch (IOException ignored) {
                // 忽略发送错误响应时的异常
            }
        }
    }
    
    private RequestWrapper parseRequest(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        
        // 读取请求行
        String line = reader.readLine();
        if (line == null) {
            return null;
        }
        
        String[] parts = line.split(" ");
        if (parts.length != 3) {
            return null;
        }
        
        RequestWrapper request = new RequestWrapper();
        request.setMethod(parts[0]);
        request.setUri(parts[1]);
        request.setProtocol(parts[2]);
        
        // 读取请求头
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            if (line.startsWith("Cookie:")) {
                parseCookies(line, request);
            }
            request.addHeader(line);
        }
        
        return request;
    }
    
    private void parseCookies(String cookieLine, RequestWrapper request) {
        String[] cookies = cookieLine.substring(8).split(";");
        for (String cookie : cookies) {
            String[] parts = cookie.trim().split("=");
            if (parts.length == 2 && "JSESSIONID".equals(parts[0])) {
                request.setSessionId(parts[1]);
                break;
            }
        }
    }
    
    private void forwardRequest(RequestWrapper request, ClusterNode node, OutputStream clientOutput) 
            throws IOException {
        String targetUrl = String.format("http://%s:%d%s", 
                                       node.getHost(), 
                                       node.getPort(), 
                                       request.getUri());
        
        URL url = new URL(targetUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(request.getMethod());
        
        // 设置请求头
        for (String header : request.getHeaders()) {
            String[] parts = header.split(": ", 2);
            if (parts.length == 2) {
                conn.setRequestProperty(parts[0], parts[1]);
            }
        }
        
        // 获取响应
        int responseCode = conn.getResponseCode();
        
        // 写入响应行
        String responseLine = String.format("HTTP/1.1 %d %s\r\n", 
                                          responseCode,
                                          conn.getResponseMessage());
        clientOutput.write(responseLine.getBytes());
        
        // 写入响应头
        conn.getHeaderFields().forEach((key, values) -> {
            if (key != null) {  // 跳过状态行
                values.forEach(value -> {
                    try {
                        clientOutput.write(String.format("%s: %s\r\n", key, value).getBytes());
                    } catch (IOException e) {
                    }
                });
            }
        });
        clientOutput.write("\r\n".getBytes());
        
        // 写入响应体
        try (InputStream responseStream = responseCode >= 400 ? 
             conn.getErrorStream() : conn.getInputStream()) {
            if (responseStream != null) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = responseStream.read(buffer)) != -1) {
                    clientOutput.write(buffer, 0, bytesRead);
                }
            }
        }
        
        clientOutput.flush();
    }
    
    private void sendError(OutputStream output, int code, String message) throws IOException {
        String response = String.format(
            "HTTP/1.1 %d %s\r\n" +
            "Content-Type: text/plain\r\n" +
            "Content-Length: %d\r\n" +
            "\r\n" +
            "%s",
            code, message, message.length(), message);
        output.write(response.getBytes());
        output.flush();
    }
} 