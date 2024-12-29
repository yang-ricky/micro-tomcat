package com.microtomcat.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;

public class Session {
    private final String id;
    private final Map<String, Object> attributes;
    private Instant lastAccessedTime;
    private final Instant creationTime;
    private int maxInactiveInterval = 1800; // 默认30分钟超时

    public Session(String id) {
        this.id = id;
        this.attributes = new ConcurrentHashMap<>();
        this.creationTime = Instant.now();
        this.lastAccessedTime = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    public void access() {
        this.lastAccessedTime = Instant.now();
    }

    public boolean isValid() {
        return Instant.now().minusSeconds(maxInactiveInterval).isBefore(lastAccessedTime);
    }

    public void setMaxInactiveInterval(int interval) {
        this.maxInactiveInterval = interval;
    }

    public Instant getCreationTime() {
        return creationTime;
    }

    public Instant getLastAccessedTime() {
        return lastAccessedTime;
    }

    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setLastAccessedTime(Instant lastAccessedTime) {
        this.lastAccessedTime = lastAccessedTime;
    }
} 