package com.microtomcat.context;

import com.microtomcat.container.Container;
import com.microtomcat.container.ContainerBase;
import com.microtomcat.container.Wrapper;
import com.microtomcat.example.SessionTestServlet;
import com.microtomcat.cluster.ClusterRegistry;
import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import com.microtomcat.connector.ServletRequestWrapper;
import com.microtomcat.connector.ServletResponseWrapper;
import com.microtomcat.lifecycle.LifecycleException;
import com.microtomcat.session.SessionManager;
import com.microtomcat.loader.WebAppClassLoader;
import com.microtomcat.loader.ClassLoaderManager;
import com.microtomcat.session.distributed.DistributedSessionManager;
import com.microtomcat.session.distributed.InMemoryReplicatedSessionStore;
import com.microtomcat.session.distributed.SessionStoreAdapter;
import com.microtomcat.session.distributed.InMemorySessionStoreAdapter;

// 添加 Servlet API 相关导入
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.util.Enumeration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.microtomcat.context.SimpleServletContext;

public class Context extends ContainerBase {
    private final String docBase;
    private final WebAppClassLoader webAppClassLoader;
    private SessionManager sessionManager;
    private Map<String, Servlet> servletMap = new ConcurrentHashMap<>();
    private ServletContext servletContext;
    private final SessionStoreAdapter sessionStore = new InMemorySessionStoreAdapter();

    public Context(String name, String docBase) throws IOException {
        this.name = name;
        this.docBase = docBase;
        
        // 创建 ServletContext
        this.servletContext = new SimpleServletContext(getName());
        
        // 创建分布式会话管理器
        ClusterRegistry clusterRegistry = ClusterRegistry.getInstance();
        SessionStoreAdapter sessionStore = new InMemoryReplicatedSessionStore(clusterRegistry, this.servletContext);
        this.sessionManager = new DistributedSessionManager(this.servletContext, sessionStore);
        
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

    public void addServlet(String name, Servlet servlet) {
        servletMap.put(name, servlet);
        
        // 创建并添加对应的 Wrapper
        Wrapper wrapper = new Wrapper(name, servlet.getClass().getName());
        wrapper.setServlet(servlet);  // 直接设置已存在的servlet实例
        addChild(wrapper);  // 添加到子容器中
        
        try {
            // 创建一个简单的 ServletConfig 实现
            ServletConfig config = new ServletConfig() {
                @Override
                public String getServletName() {
                    return name;
                }

                @Override
                public ServletContext getServletContext() {
                    return null; // TODO: 实现 ServletContext
                }

                @Override
                public String getInitParameter(String name) {
                    return null;
                }

                @Override
                public Enumeration<String> getInitParameterNames() {
                    return new Enumeration<String>() {
                        @Override
                        public boolean hasMoreElements() {
                            return false;
                        }

                        @Override
                        public String nextElement() {
                            return null;
                        }
                    };
                }
            };
            servlet.init(config);
        } catch (ServletException e) {
            log("Failed to initialize servlet: " + name);
        }
    }

    @Override
    public void invoke(Request request, Response response) {
        // 优先处理 dispatcherServlet
        Servlet dispatcher = servletMap.get("dispatcherServlet");
        if (dispatcher != null) {
            try {
                // 将 Request/Response 适配成 ServletRequest/ServletResponse
                ServletRequestWrapper servletRequest = new ServletRequestWrapper(request);
                ServletResponseWrapper servletResponse = new ServletResponseWrapper(response);
                
                dispatcher.service(servletRequest, servletResponse);
                return; // 处理完请求，就直接return
            } catch (NoRouteMatchException e) {
                // 继续后面静态资源处理
            } catch (ServletException | IOException e) {
                log("Error invoking dispatcherServlet: " + e.getMessage());
            }
        }

        // 原有的静态资源或其他 servlet 处理逻辑
        request.setContext(this);
        String servletPath = getServletPath(request.getUri());
        if (servletPath != null && servletPath.startsWith("/servlet/")) {
            String servletName = servletPath.substring("/servlet/".length());
            servletName = servletName.replace('/', '.');
            try {
                Wrapper wrapper = (Wrapper) findChild(servletName);
                if (wrapper == null) {
                    wrapper = new Wrapper(servletName, servletName.substring(servletName.lastIndexOf('.') + 1));
                    addChild(wrapper);
                    wrapper.init();
                    wrapper.start();
                }
                wrapper.invoke(request, response);
            } catch (Exception e) {
                log("Error processing request: " + e.getMessage());
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

    public ServletContext getServletContext() {
        if (servletContext == null) {
            servletContext = new SimpleServletContext(getName());
        }
        return servletContext;
    }

    public String getRealPath(String path) {
        // 返回 webroot 目录下对应路径的实际文件系统路径
        return System.getProperty("user.dir") + "/webroot" + path;
    }

    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
        if (this.servletContext != null) {
            // 确保使用正确的 ServletContext
            this.sessionManager = new SessionManager(this.servletContext);
        }
    }
} 