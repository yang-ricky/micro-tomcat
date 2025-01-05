package com.microtomcat.connector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.Enumeration;

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
    private final StringBuilder requestContent = new StringBuilder();
    private BufferedReader reader;
    private String body;
    private final Map<String, String[]> parameters = new HashMap<>();
    private final Map<String, Object> attributes = new HashMap<>();
    private int serverPort = 8080;
    private String remoteAddr;
    private String scheme = "http";

    public Request(InputStream input, SessionManager sessionManager) {
        this.input = input;
        this.sessionManager = sessionManager;
        this.reader = new BufferedReader(new InputStreamReader(input));
    }

    public void parse() throws IOException {
        String requestLine = reader.readLine();
        if (requestLine != null) {
            String[] parts = requestLine.split(" ");
            if (parts.length == 3) {
                method = parts[0];
                uri = parts[1];
                protocol = parts[2];
            }
        }
        
        String headerLine;
        while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
            int colonPos = headerLine.indexOf(':');
            if (colonPos > 0) {
                String headerName = headerLine.substring(0, colonPos).trim();
                String headerValue = headerLine.substring(colonPos + 1).trim();
                headers.put(headerName, headerValue);
            }
        }

        if ("POST".equalsIgnoreCase(method)) {
            String contentLengthStr = headers.get("Content-Length");
            if (contentLengthStr != null) {
                int contentLength = Integer.parseInt(contentLengthStr);
                char[] bodyChars = new char[contentLength];
                int readCount = reader.read(bodyChars);
                if (readCount > 0) {
                    this.body = new String(bodyChars, 0, readCount);
                }
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

    public BufferedReader getReader() {
        return reader;
    }

    public String getBody() {
        return body;
    }

    public String getParameter(String name) {
        String[] values = parameters.get(name);
        return values != null && values.length > 0 ? values[0] : null;
    }

    public String getContentType() {
        return headers.get("Content-Type");
    }

    public int getContentLength() {
        String length = headers.get("Content-Length");
        return length != null ? Integer.parseInt(length) : -1;
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameters.keySet());
    }

    public String[] getParameterValues(String name) {
        return parameters.get(name);
    }

    public Map<String, String[]> getParameterMap() {
        return Collections.unmodifiableMap(parameters);
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getRemoteAddr() {
        return remoteAddr != null ? remoteAddr : "127.0.0.1";
    }

    public String getRemoteHost() {
        return getRemoteAddr();
    }

    public int getRemotePort() {
        return 0; // 默认值
    }

    public String getLocalName() {
        return "localhost";
    }

    public String getLocalAddr() {
        return "127.0.0.1";
    }

    public int getLocalPort() {
        return serverPort;
    }

    public String getScheme() {
        return scheme;
    }

    public void setParameter(String name, String value) {
        parameters.put(name, new String[]{value});
    }

    public void setParameter(String name, String[] values) {
        parameters.put(name, values);
    }
}
