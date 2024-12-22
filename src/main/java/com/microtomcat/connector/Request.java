package com.microtomcat.connector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.microtomcat.session.Session;
import com.microtomcat.session.SessionManager;
import com.microtomcat.context.Context;

public class Request {
    private InputStream input;
    private String method;
    private String uri;
    private String protocol;
    private Session session;
    private final SessionManager sessionManager;
    private static final String SESSION_COOKIE_NAME = "JSESSIONID";
    private final Map<String, String> headers = new HashMap<>();
    private Context context;
    private String serverName;

    public Request(InputStream input, SessionManager sessionManager) {
        this.input = input;
        this.sessionManager = sessionManager;
    }

    public void parse() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        
        // 解析请求行
        String requestLine = reader.readLine();
        if (requestLine != null) {
            String[] parts = requestLine.split(" ");
            if (parts.length == 3) {
                method = parts[0];
                uri = parts[1];
                protocol = parts[2];
            }
        }
        
        // 解析请求头
        String headerLine;
        while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
            int colonPos = headerLine.indexOf(':');
            if (colonPos > 0) {
                String headerName = headerLine.substring(0, colonPos).trim();
                String headerValue = headerLine.substring(colonPos + 1).trim();
                headers.put(headerName, headerValue);
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

        String sessionId = getCookieValue(SESSION_COOKIE_NAME);
        if (sessionId != null && context != null) {
            session = context.getSessionManager().getSession(sessionId);
            if (session != null) {
                return session;
            }
        }

        if (create && context != null) {
            session = context.getSessionManager().createSession();
            return session;
        }

        return null;
    }

    private String getCookieValue(String name) {
        try {
            // 从请求头中查找 Cookie
            String cookieHeader = headers.get("Cookie");
            if (cookieHeader != null) {
                String[] cookies = cookieHeader.split(";");
                for (String cookie : cookies) {
                    String[] parts = cookie.trim().split("=");
                    if (parts.length == 2 && parts[0].trim().equals(name)) {
                        return parts[1].trim();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    public String getServerName() {
        if (serverName == null) {
            // 从Host头中获取
            serverName = getHeader("Host");
            if (serverName != null) {
                int colonPos = serverName.indexOf(':');
                if (colonPos != -1) {
                    serverName = serverName.substring(0, colonPos);
                }
            }
        }
        return serverName;
    }
}
