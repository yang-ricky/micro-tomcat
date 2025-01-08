package com.microtomcat.context;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.microtomcat.container.Context;
import com.microtomcat.lifecycle.LifecycleBase;
import com.microtomcat.lifecycle.LifecycleException;
public class ContextManager extends LifecycleBase {
    private final Map<String, Context> contexts = new ConcurrentHashMap<>();
    private final String webRoot;

    public ContextManager(String webRoot) {
        this.webRoot = webRoot;
    }

    @Override
    protected void initInternal() throws LifecycleException {
        log("Initializing ContextManager");
        // 初始化所有上下文
        for (Context context : contexts.values()) {
            context.init();
        }
    }

    @Override
    protected void startInternal() throws LifecycleException {
        log("Starting ContextManager");
        for (Context context : contexts.values()) {
            context.start();
        }
    }

    @Override
    protected void stopInternal() throws LifecycleException {
        log("Stopping ContextManager");
        for (Context context : contexts.values()) {
            context.stop();
        }
    }

    @Override
    protected void destroyInternal() throws LifecycleException {
        log("Destroying ContextManager");
        for (Context context : contexts.values()) {
            context.destroy();
        }
        contexts.clear();
    }

    public void createContext(String contextPath, String docBase) {
        try {
            Context context = new Context(contextPath, docBase);
            contexts.put(contextPath, context);
            log("Created context: " + contextPath + ", docBase: " + docBase);
        } catch (IOException e) {
            log("Failed to create context: " + contextPath + ", error: " + e.getMessage());
        }
    }

    public Context getContext(String uri) {
        // 首先尝试精确匹配
        for (Map.Entry<String, Context> entry : contexts.entrySet()) {
            String contextPath = entry.getKey();
            if (!contextPath.isEmpty() && uri.startsWith(contextPath)) {
                return entry.getValue();
            }
        }
        // 如果没有找到匹配的应用上下文，返回根上下文
        return contexts.get("");
    }

    private void log(String message) {
        System.out.println("[ContextManager] " + message);
    }
}