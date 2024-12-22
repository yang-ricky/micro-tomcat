package com.microtomcat.context;

import com.microtomcat.lifecycle.LifecycleBase;
import com.microtomcat.lifecycle.LifecycleException;
import com.microtomcat.servlet.ServletLoader;
import com.microtomcat.session.SessionManager;
import java.io.File;
import java.io.IOException;

public class Context extends LifecycleBase {
    private final String contextPath;
    private final String docBase;
    private final ServletLoader servletLoader;
    private final SessionManager sessionManager;

    public Context(String contextPath, String docBase) throws IOException {
        this.contextPath = contextPath;
        this.docBase = docBase;
        this.sessionManager = new SessionManager();
        
        // 为每个上下文创建独立的 ServletLoader
        String classesPath = docBase + "/WEB-INF/classes";
        this.servletLoader = new ServletLoader(docBase, classesPath);
    }

    public String getContextPath() {
        return contextPath;
    }

    public String getDocBase() {
        return docBase;
    }

    public ServletLoader getServletLoader() {
        return servletLoader;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    @Override
    protected void initInternal() throws LifecycleException {
        log("Initializing context: " + contextPath);
        // 初始化 ServletLoader
        // 初始化 SessionManager
    }

    @Override
    protected void startInternal() throws LifecycleException {
        log("Starting context: " + contextPath);
        // 启动 ServletLoader
        // 启动 SessionManager
    }

    @Override
    protected void stopInternal() throws LifecycleException {
        log("Stopping context: " + contextPath);
        // 停止 ServletLoader
        // 停止 SessionManager
    }

    @Override
    protected void destroyInternal() throws LifecycleException {
        log("Destroying context: " + contextPath);
        // 清理资源
    }

    private void log(String message) {
        System.out.println("[Context] " + message);
    }
} 