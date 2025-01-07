package com.microtomcat.context;

import com.microtomcat.container.Container;
import com.microtomcat.container.ContainerBase;
import com.microtomcat.container.Wrapper;
import com.microtomcat.cluster.ClusterRegistry;
import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import com.microtomcat.connector.ServletRequestWrapper;
import com.microtomcat.connector.ServletResponseWrapper;
import com.microtomcat.lifecycle.Lifecycle;
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

// 添加缺失的 IO 相关导入
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletResponse;

public class Context extends ContainerBase {
    private final String docBase;
    private final WebAppClassLoader webAppClassLoader;
    private SessionManager sessionManager;
    private Map<String, Servlet> servletMap = new ConcurrentHashMap<>();
    private ServletContext servletContext;
    private final SessionStoreAdapter sessionStore = new InMemorySessionStoreAdapter();
    private final Map<String, String> servletMappings = new ConcurrentHashMap<>();

    public Context(String name, String docBase) throws IOException {
        this.name = name;
        this.docBase = docBase;
        
        // 创建基本的 ServletContext
        SimpleServletContext simpleContext = new SimpleServletContext(getName());
        simpleContext.setAttribute("webRoot", docBase);  // 存储 webRoot 路径
        this.servletContext = simpleContext;
        
        // 创建分布式会话管理器
        ClusterRegistry clusterRegistry = ClusterRegistry.getInstance();
        SessionStoreAdapter sessionStore = new InMemoryReplicatedSessionStore(clusterRegistry, this.servletContext);
        this.sessionManager = new DistributedSessionManager(this.servletContext, sessionStore);
        
        // 初始化类加载器
        this.webAppClassLoader = ClassLoaderManager.createWebAppClassLoader(docBase);
    }

    private void registerDefaultServlets() {
        try {
            // 根上下文
            if (name.equals("")) {
                // 注册默认的 DefaultServlet 处理静态资源
                log("Registering DefaultServlet...");
                Wrapper defaultWrapper = new Wrapper("default", "com.microtomcat.servlet.DefaultServlet");
                addChild(defaultWrapper);
                
                // 初始化和启动 Wrapper
                defaultWrapper.init();
                defaultWrapper.start();
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

    @Override
    public WebAppClassLoader getWebAppClassLoader() {
        return webAppClassLoader;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public void addServlet(String servletName, Servlet servlet) {
        // 创建 Wrapper
        Wrapper wrapper = new Wrapper(servletName, servlet.getClass().getName());
        wrapper.setServlet(servlet);
        wrapper.setParent(this);
        
        // 添加到子容器
        addChild(wrapper);
        
        // 如果是 dispatcherServlet，也添加到 servletMap
        if ("dispatcherServlet".equals(servletName)) {
            servletMap.put(servletName, servlet);
        }

        // 初始化 servlet
        try {
            ServletConfig config = new ServletConfig() {
                @Override
                public String getServletName() {
                    return servletName;
                }

                @Override
                public ServletContext getServletContext() {
                    return Context.this.getServletContext();
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
            log("Failed to initialize servlet: " + servletName);
        }
    }

    @Override
    public void invoke(Request request, Response response) {
        // 优先处理 dispatcherServlet
        Servlet dispatcher = servletMap.get("dispatcherServlet");
        if (dispatcher != null) {
            try {
                log("Using dispatcherServlet");
                dispatcher.service(request, response);
                response.flushBuffer();  // 确保响应被刷新
                return;
            } catch (Exception e) {
                log("Error invoking dispatcherServlet: " + e.getMessage());
            }
        }

        // 查找匹配的 servlet
        String uri = request.getRequestURI();
        log("Looking for servlet mapping for URI: " + uri);
        String mappedServletName = findServletMapping(uri);
        if (mappedServletName != null) {
            log("Found servlet mapping: " + mappedServletName);
            Container wrapper = findChild(mappedServletName);
            if (wrapper instanceof Wrapper) {
                try {
                    log("Invoking servlet: " + mappedServletName);
                    ((Wrapper) wrapper).service(request, response);
                    if (!response.isCommitted()) {
                        response.setStatus(HttpServletResponse.SC_OK);
                    }
                    response.flushBuffer();  // 确保响应被刷新
                    return;
                } catch (Exception e) {
                    log("Error invoking servlet: " + e.getMessage());
                }
            }
        }

        // 处理静态资源
        try {
            String relativePath = uri.startsWith("/") ? uri.substring(1) : uri;
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
        try {
            // 初始化 ServletContext
            servletContext.setAttribute("docBase", docBase);
            
            // 创建并注册 DefaultServlet
            Wrapper defaultWrapper = new Wrapper("default", 
                "com.microtomcat.servlet.DefaultServlet");
            addChild(defaultWrapper);
            
            // 不再需要初始化 webAppClassLoader，因为它在构造函数中已经初始化
            
        } catch (Exception e) {
            throw new LifecycleException("Error initializing context", e);
        }
    }

    @Override
    protected void startInternal() throws LifecycleException {
        try {
            // 在启动时注册默认 servlet
            registerDefaultServlets();
            
            // 启动所有子容器
            Container[] children = findChildren();
            for (Container child : children) {
                if (!Lifecycle.STARTED.equals(child.getState())) {
                    child.start();
                }
            }
        } catch (Exception e) {
            throw new LifecycleException("Error starting context", e);
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

    public void service(Request request, Response response) throws IOException, ServletException {
        try {
            String uri = request.getRequestURI();
            if (uri == null) {
                uri = "/";
            }
            
            // 先尝试查找并调用 Servlet
            String mappedServletName = findServletMapping(uri);
            if (mappedServletName != null) {
                Container wrapper = findChild(mappedServletName);
                if (wrapper instanceof Wrapper) {
                    wrapper.invoke(request, response);
                    return;
                }
            }
            
            // 如果没有找到匹配的 Servlet，再处理欢迎页面
            if (uri.equals("/") || uri.equals("/index.html")) {
                ServletResponseWrapper wrappedResponse = new ServletResponseWrapper(response);
                // ... 原有的欢迎页面处理逻辑 ...
            }
        } catch (Exception e) {
            System.err.println("ERROR in Context.service: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
        // 如果设置了新的 ServletContext，确保它有 webRoot 属性
        if (servletContext.getAttribute("webRoot") == null) {
            servletContext.setAttribute("webRoot", this.docBase);
        }
    }

    /**
     * 添加 Servlet 映射
     * @param urlPattern URL 模式，如 "/hello", "*.jsp", "/*" 等
     * @param servletName Servlet 的名称
     */
    public void addServletMapping(String urlPattern, String servletName) {
        // 验证参数
        if (urlPattern == null || servletName == null) {
            throw new IllegalArgumentException("urlPattern and servletName must not be null");
        }

        // 规范化 URL 模式
        if (!urlPattern.startsWith("/") && !urlPattern.startsWith("*")) {
            urlPattern = "/" + urlPattern;
        }

        // 存储映射
        servletMappings.put(urlPattern, servletName);
        log("Added servlet mapping: " + urlPattern + " -> " + servletName);
    }

    /**
     * 根据 URL 查找对应的 Servlet
     * @param uri 请求的 URI
     * @return 匹配的 Servlet 名称，如果没有匹配返回 null
     */
    protected String findServletMapping(String uri) {
        // 1. 精确匹配
        String servletName = servletMappings.get(uri);
        if (servletName != null) {
            return servletName;
        }

        // 2. 路径匹配（最长匹配优先）
        String longestMatch = "";
        String matchedServlet = null;
        for (Map.Entry<String, String> entry : servletMappings.entrySet()) {
            String pattern = entry.getKey();
            if (pattern.endsWith("/*")) {
                String prefix = pattern.substring(0, pattern.length() - 2);
                if (uri.startsWith(prefix) && prefix.length() > longestMatch.length()) {
                    longestMatch = prefix;
                    matchedServlet = entry.getValue();
                }
            }
        }
        if (matchedServlet != null) {
            return matchedServlet;
        }

        // 3. 扩展名匹配
        int lastDot = uri.lastIndexOf('.');
        if (lastDot >= 0) {
            String extension = "*" + uri.substring(lastDot);
            servletName = servletMappings.get(extension);
            if (servletName != null) {
                return servletName;
            }
        }

        // 4. 默认匹配 "/*"
        return servletMappings.get("/*");
    }
} 