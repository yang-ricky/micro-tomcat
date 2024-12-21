package com.microtomcat.context;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ContextManager {
    private final Map<String, Context> contexts = new ConcurrentHashMap<>();
    private final String webRoot;

    public ContextManager(String webRoot) {
        this.webRoot = webRoot;
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
        System.out.println(message);
    }
}