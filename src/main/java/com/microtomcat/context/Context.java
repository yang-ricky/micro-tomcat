package com.microtomcat.context;

import com.microtomcat.container.Container;
import com.microtomcat.container.ContainerBase;
import com.microtomcat.container.Wrapper;
import com.microtomcat.example.SessionTestServlet;
import com.microtomcat.cluster.ClusterRegistry;
import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import com.microtomcat.lifecycle.LifecycleException;
import com.microtomcat.session.SessionManager;
import com.microtomcat.loader.WebAppClassLoader;
import com.microtomcat.loader.ClassLoaderManager;
import com.microtomcat.session.distributed.DistributedSessionManager;
import com.microtomcat.session.distributed.InMemoryReplicatedSessionStore;
import com.microtomcat.session.distributed.SessionStoreAdapter;
import java.io.File;
import java.io.IOException;

public class Context extends ContainerBase {
    private final String docBase;
    private final WebAppClassLoader webAppClassLoader;
    private final SessionManager sessionManager;

    public Context(String name, String docBase) throws IOException {
        this.name = name;
        this.docBase = docBase;
        
        // 创建分布式会话管理器
        ClusterRegistry clusterRegistry = ClusterRegistry.getInstance();
        SessionStoreAdapter sessionStore = new InMemoryReplicatedSessionStore(clusterRegistry);
        this.sessionManager = new DistributedSessionManager(sessionStore);
        
        this.webAppClassLoader = ClassLoaderManager.createWebAppClassLoader(docBase);
        
        registerDefaultServlets();
    }

    private void registerDefaultServlets() {
        try {
            // 根据上下文路径注册不同的 Servlet
            if (name.equals("")) {  // 根上下文
                // 注册 HelloServlet
                log("Registering HelloServlet...");
                Wrapper helloWrapper = new Wrapper("HelloServlet", "com.microtomcat.example.HelloServlet");
                Wrapper sessionTestWrapper = new Wrapper("SessionTestServlet", "com.microtomcat.example.SessionTestServlet");
                addChild(helloWrapper);
                addChild(sessionTestWrapper);
                Wrapper webRootWrapper = new Wrapper("WebRootServlet", "WebRootServlet");
                addChild(webRootWrapper);
            } else if (name.equals("/app1")) {  // app1 上下文
                // 注册 App1Servlet
                log("Registering App1Servlet...");
                Wrapper app1Wrapper = new Wrapper("App1Servlet", "App1Servlet");
                addChild(app1Wrapper);
            } else if (name.equals("/app2")) {  // app2 上下文
                // 注册 App2Servlet
                log("Registering App2Servlet...");
                Wrapper app2Wrapper = new Wrapper("App2Servlet", "App2Servlet");
                addChild(app2Wrapper);
            }

            log("Successfully registered servlets for context: " + name);
        } catch (Exception e) {
            log("Error registering servlets: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String getDocBase() {
        return docBase;
    }

    public WebAppClassLoader getWebAppClassLoader() {
        return webAppClassLoader;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    @Override
    public void invoke(Request request, Response response) {
        request.setContext(this);
        
        String servletPath = getServletPath(request.getUri());
        if (servletPath != null && servletPath.startsWith("/servlet/")) {
            String servletName = servletPath.substring("/servlet/".length());
            // 移除路径分隔符，只保留 Servlet 名称
            servletName = servletName.replace('/', '.');
            try {
                Wrapper wrapper = (Wrapper) findChild(servletName);
                if (wrapper == null) {
                    // 动态创建Wrapper，使用简单的类名
                    wrapper = new Wrapper(servletName, servletName.substring(servletName.lastIndexOf('.') + 1));
                    addChild(wrapper);
                    wrapper.init();
                    wrapper.start();
                }
                wrapper.invoke(request, response);
            } catch (Exception e) {
                log("Error processing servlet request: " + e.getMessage());
                try {
                    response.sendError(500, "Internal Server Error: " + e.getMessage());
                } catch (IOException ioe) {
                    log("Failed to send error response: " + ioe.getMessage());
                }
            }
        } else {
            // 处理静态资源
            try {
                String relativePath = request.getUri();
                // 如果URI以上下文路径开头，移除它
                if (relativePath.startsWith(name) && !name.equals("/")) {
                    relativePath = relativePath.substring(name.length());
                }
                // 如果路径是目录，默认查找 index.html
                if (relativePath.endsWith("/")) {
                    relativePath += "index.html";
                }
                
                File file = new File(docBase, relativePath);
                if (file.exists() && file.isFile()) {
                    response.sendStaticResource(file);
                } else {
                    response.sendError(404, "File Not Found: " + relativePath);
                }
            } catch (IOException e) {
                log("Error sending static resource: " + e.getMessage());
                try {
                    response.sendError(500, "Internal Server Error: " + e.getMessage());
                } catch (IOException ioe) {
                    log("Failed to send error response: " + ioe.getMessage());
                }
            }
        }
    }

    private String getServletPath(String uri) {
        if (uri == null) {
            return null;
        }
        // 如果URI以上下文路径开头，移除它
        if (uri.startsWith(name) && !name.equals("/")) {
            return uri.substring(name.length());
        }
        return uri;
    }

    @Override
    protected void initInternal() throws LifecycleException {
        log("Initializing context: " + name);
        // 初始化所有子容器（包括 Servlet Wrapper）
        Container[] children = findChildren();
        for (Container child : children) {
            child.init();
        }
    }

    @Override
    protected void startInternal() throws LifecycleException {
        log("Starting context: " + name);
        Container[] children = findChildren();
        for (Container child : children) {
            child.start();
        }
    }

    @Override
    protected void stopInternal() throws LifecycleException {
        log("Stopping context: " + name);
        Container[] children = findChildren();
        for (Container child : children) {
            child.stop();
        }
    }

    @Override
    protected void destroyInternal() throws LifecycleException {
        log("Destroying context: " + name);
        webAppClassLoader.destroy();
    }
} 