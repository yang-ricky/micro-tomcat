package com.microtomcat.connector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.microtomcat.session.Session;
import com.microtomcat.session.SessionManager;

public class Request {
    private InputStream input;
    private String method;
    private String uri;
    private String protocol;
    private Session session;
    private final SessionManager sessionManager;
    private static final String SESSION_COOKIE_NAME = "JSESSIONID";

    public Request(InputStream input, SessionManager sessionManager) {
        this.input = input;
        this.sessionManager = sessionManager;
    }

    public void parse() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        String requestLine = reader.readLine();
        
        if (requestLine != null) {
            String[] parts = requestLine.split(" ");
            if (parts.length == 3) {
                method = parts[0];
                uri = parts[1];
                protocol = parts[2];
            }
        }
    }

    public String getMethod() {
        return method;
    }

    public String getUri() {
        return uri;
    }

    public String getProtocol() {
        return protocol;
    }

    public Session getSession() {
        return getSession(true);
    }

    public Session getSession(boolean create) {
        if (session != null) {
            return session;
        }

        // 从 Cookie 中获取会话 ID
        String sessionId = getCookieValue(SESSION_COOKIE_NAME);
        if (sessionId != null) {
            session = sessionManager.getSession(sessionId);
            if (session != null) {
                return session;
            }
        }

        if (create) {
            session = sessionManager.createSession();
            return session;
        }

        return null;
    }

    private String getCookieValue(String name) {
        // 实现从请求头中获取 Cookie 值的逻辑
        // 这里需要解析 Cookie 请求头
        return null; // 临时返回
    }
}
