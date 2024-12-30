package com.microtomcat.gateway.model;

import java.util.ArrayList;
import java.util.List;

public class RequestWrapper {
    private String method;
    private String uri;
    private String protocol;
    private String sessionId;
    private final List<String> headers = new ArrayList<>();
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method;
    }
    
    public String getUri() {
        return uri;
    }
    
    public void setUri(String uri) {
        this.uri = uri;
    }
    
    public String getProtocol() {
        return protocol;
    }
    
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public List<String> getHeaders() {
        return headers;
    }
    
    public void addHeader(String header) {
        headers.add(header);
    }
} 