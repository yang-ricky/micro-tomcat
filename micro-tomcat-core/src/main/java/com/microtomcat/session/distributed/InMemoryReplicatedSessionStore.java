package com.microtomcat.session.distributed;

import com.microtomcat.session.Session;
import com.microtomcat.cluster.ClusterNode;
import com.microtomcat.cluster.ClusterRegistry;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;
import java.util.Map;
import java.util.HashMap;
import com.microtomcat.session.StandardSession;
import javax.servlet.ServletContext;

public class InMemoryReplicatedSessionStore implements SessionStoreAdapter {
    private final ConcurrentHashMap<String, Session> localSessions = new ConcurrentHashMap<>();
    private final ClusterRegistry clusterRegistry;
    private final ServletContext servletContext;
    
    public InMemoryReplicatedSessionStore(ClusterRegistry clusterRegistry, ServletContext servletContext) {
        this.clusterRegistry = clusterRegistry;
        this.servletContext = servletContext;
    }

    @Override
    public void saveSession(Session session) {
        localSessions.put(session.getId(), session);
        replicateSessionToOthers(session);
    }

    @Override
    public Session loadSession(String sessionId) {
        Session session = localSessions.get(sessionId);
        if (session != null && !session.isValid()) {
            deleteSession(sessionId);
            return null;
        }
        return session;
    }

    @Override
    public void deleteSession(String sessionId) {
        localSessions.remove(sessionId);
        replicateDeleteToOthers(sessionId);
    }

    private void replicateSessionToOthers(Session session) {
        String sessionJson = sessionToJson(session);
        
        for (ClusterNode node : clusterRegistry.getAllNodes()) {
            if (isCurrentNode(node)) continue;
            
            HttpURLConnection conn = null;
            try {
                String url = String.format("http://%s:%d/_sessionReplication", 
                    node.getHost(), node.getPort());
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(1000);
                conn.setReadTimeout(1000);
                
                String body = "ACTION=SAVE\n" + sessionJson;
                
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes());
                    os.flush();
                }
                
                if (conn.getResponseCode() != 200) {
                    System.err.println("Failed to replicate session to " + url + 
                        ", response code: " + conn.getResponseCode());
                }
            } catch (Exception e) {
                System.err.println("Warning: Failed to replicate session to node: " + 
                    node.getName() + " (" + e.getMessage() + ")");
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
    }

    private void replicateDeleteToOthers(String sessionId) {
        for (ClusterNode node : clusterRegistry.getAllNodes()) {
            if (isCurrentNode(node)) continue;
            
            HttpURLConnection conn = null;
            try {
                String url = String.format("http://%s:%d/_sessionReplication", 
                    node.getHost(), node.getPort());
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(1000);
                conn.setReadTimeout(1000);
                
                String body = "ACTION=DELETE\nsessionId=" + sessionId;
                
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes());
                    os.flush();
                }
                
                if (conn.getResponseCode() != 200) {
                    System.err.println("Failed to replicate session deletion to " + url);
                }
            } catch (Exception e) {
                System.err.println("Warning: Failed to replicate session deletion to node: " + 
                    node.getName() + " (" + e.getMessage() + ")");
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
    }

    private boolean isCurrentNode(ClusterNode node) {
        ClusterNode currentNode = clusterRegistry.getCurrentNode();
        if (currentNode == null) {
            return false;
        }
        return node.getHost().equals(currentNode.getHost()) 
            && node.getPort() == currentNode.getPort();
    }

    private String sessionToJson(Session session) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"id\":\"").append(session.getId()).append("\",");
        json.append("\"creationTime\":\"").append(session.getCreationTime()).append("\",");
        json.append("\"lastAccessedTime\":\"").append(session.getLastAccessedTime()).append("\",");
        json.append("\"maxInactiveInterval\":").append(session.getMaxInactiveInterval()).append(",");
        
        // 序列化属性
        json.append("\"attributes\":{");
        Map<String, Object> attributes = session.getAttributes();
        if (attributes != null && !attributes.isEmpty()) {
            boolean first = true;
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                if (!first) json.append(",");
                json.append("\"").append(entry.getKey()).append("\":\"")
                    .append(entry.getValue().toString()).append("\"");
                first = false;
            }
        }
        json.append("}");
        
        json.append("}");
        return json.toString();
    }

    public void saveSessionLocally(Session session) {
        localSessions.put(session.getId(), session);
    }

    public void deleteSessionLocally(String sessionId) {
        localSessions.remove(sessionId);
    }

    public Session jsonToSession(String json) {
        // 移除首尾的大括号
        json = json.substring(1, json.length() - 1);
        
        // 创建一个简单的属性映射
        Map<String, String> props = new HashMap<>();
        
        // 分割每个键值对
        for (String pair : json.split(",")) {
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length == 2) {
                // 清理引号
                String key = keyValue[0].replaceAll("\"", "").trim();
                String value = keyValue[1].replaceAll("\"", "").trim();
                props.put(key, value);
            }
        }
        
        // 创建新的会话
        StandardSession session = new StandardSession(props.get("id"), servletContext);
        session.setLastAccessedTime(Instant.parse(props.get("lastAccessedTime")));
        session.setMaxInactiveInterval(Integer.parseInt(props.get("maxInactiveInterval")));
        
        // 处理属性（如果存在）
        String attributesStr = props.get("attributes");
        if (attributesStr != null && attributesStr.length() > 2) {
            // 移除属性对象的大括号
            attributesStr = attributesStr.substring(1, attributesStr.length() - 1);
            for (String pair : attributesStr.split(",")) {
                String[] keyValue = pair.split(":", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].replaceAll("\"", "").trim();
                    String value = keyValue[1].replaceAll("\"", "").trim();
                    session.setAttribute(key, value);
                }
            }
        }
        
        return session;
    }

    private Session createSession(String id, Map<String, Object> attributes) {
        StandardSession session = new StandardSession(id, servletContext);
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            session.setAttribute(entry.getKey(), entry.getValue());
        }
        return session;
    }
} 