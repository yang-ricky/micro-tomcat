package com.microtomcat.session;

import javax.servlet.ServletContext;
import java.time.Instant;
import java.util.Enumeration;
import java.util.Map;

public interface Session {
    long getCreationTime();
    String getId();
    long getLastAccessedTime();
    ServletContext getServletContext();
    void setMaxInactiveInterval(int interval);
    int getMaxInactiveInterval();
    Object getAttribute(String name);
    void setAttribute(String name, Object value);
    void removeAttribute(String name);
    Enumeration<String> getAttributeNames();
    void invalidate();
    boolean isNew();
    boolean isValid();
    void access();
    Map<String, Object> getAttributes();
    void setLastAccessedTime(Instant time);
    // 添加兼容性方法
    default void removeValue(String name) {
        removeAttribute(name);
    }
} 