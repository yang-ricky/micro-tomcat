package com.microtomcat.session;

import javax.servlet.ServletContext;
import java.time.Instant;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class StandardSession implements Session {
    private final String id;
    private final Instant creationTime;
    private Instant lastAccessedTime;
    private boolean isNew;
    private boolean isValid;
    private int maxInactiveInterval;
    private final ServletContext servletContext;
    private final Map<String, Object> attributes;

    public StandardSession(String id, ServletContext servletContext) {
        this.id = id;
        this.servletContext = servletContext;
        this.creationTime = Instant.now();
        this.lastAccessedTime = Instant.now();
        this.isNew = true;
        this.isValid = true;
        this.attributes = new HashMap<>();
        this.maxInactiveInterval = 1800; // 30 minutes default
    }

    @Override
    public long getCreationTime() {
        return creationTime.toEpochMilli();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getLastAccessedTime() {
        return lastAccessedTime.toEpochMilli();
    }

    public void setLastAccessedTime(Instant time) {
        this.lastAccessedTime = time;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        this.maxInactiveInterval = interval;
    }

    @Override
    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public void invalidate() {
        isValid = false;
        attributes.clear();
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    public boolean isValid() {
        return isValid;
    }

    public void access() {
        this.lastAccessedTime = Instant.now();
        this.isNew = false;
    }

    public Map<String, Object> getAttributes() {
        return new HashMap<>(attributes);
    }
} 