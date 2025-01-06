package com.microtomcat.connector;

import com.microtomcat.session.Session;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletContext;
import java.util.Enumeration;
import java.util.List;
import java.util.Collections;

public class HttpSessionWrapper implements HttpSession {
    private final Session session;

    public HttpSessionWrapper(Session session) {
        this.session = session;
    }

    @Override
    public long getCreationTime() {
        return session.getCreationTime();
    }

    @Override
    public String getId() {
        return session.getId();
    }

    @Override
    public long getLastAccessedTime() {
        return session.getLastAccessedTime();
    }

    @Override
    public ServletContext getServletContext() {
        return session.getServletContext();
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        session.setMaxInactiveInterval(interval);
    }

    @Override
    public int getMaxInactiveInterval() {
        return session.getMaxInactiveInterval();
    }

    @Override
    public Object getAttribute(String name) {
        return session.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return session.getAttributeNames();
    }

    @Override
    public void setAttribute(String name, Object value) {
        session.setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        session.removeAttribute(name);
    }

    @Override
    public void invalidate() {
        session.invalidate();
    }

    @Override
    public boolean isNew() {
        return session.isNew();
    }

    @Override
    public void removeValue(String name) {
        removeAttribute(name);
    }

    @Override
    public void putValue(String name, Object value) {
        setAttribute(name, value);
    }

    @Override
    public String[] getValueNames() {
        Enumeration<String> names = getAttributeNames();
        List<String> nameList = Collections.list(names);
        return nameList.toArray(new String[0]);
    }

    @Override
    public Object getValue(String name) {
        return getAttribute(name);
    }

    @Override
    public javax.servlet.http.HttpSessionContext getSessionContext() {
        return null;
    }
} 