package com.microtomcat.context;

import com.microtomcat.servlet.ServletLoader;
import com.microtomcat.session.SessionManager;
import java.io.File;
import java.io.IOException;

public class Context {
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
} 