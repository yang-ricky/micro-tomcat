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

    public Context(String name, String docBase) throws IOException {
        this.name = name;
        this.docBase = docBase;
        
        // 创建基本的 ServletContext
        SimpleServletContext simpleContext = new SimpleServletContext(getName());
        simpleContext.setAttribute("webRoot", docBase);  // 存储 webRoot 路径
        this.servletContext = simpleContext;
        
        // 创建类加载器
        this.webAppClassLoader = ClassLoaderManager.createWebAppClassLoader(docBase);
        
        // 创建会话管理器
        ClusterRegistry clusterRegistry = ClusterRegistry.getInstance();
        this.sessionManager = new DistributedSessionManager(this.servletContext, sessionStore);
        
        // 注意：不在构造函数中调用 registerDefaultServlets()
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
                
                // 注册其他 Servlet
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

    @Override
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
            
            ServletResponseWrapper wrappedResponse = new ServletResponseWrapper(response);
            
            // 处理根路径请求
            if (uri.equals("/") || uri.equals("/index.html")) {
                System.out.println("DEBUG Context : Handling root path request: " + uri);
                if (!response.isCommitted()) {
                    wrappedResponse.setStatus(HttpServletResponse.SC_OK);
                    wrappedResponse.setContentType("text/html");
                    wrappedResponse.setHeader("Server", "MicroTomcat");
                    
                    String content = "<!DOCTYPE html>\n"
                        + "<html>\n"
                        + "<head>\n"
                        + "    <title>Welcome to MicroTomcat</title>\n"
                        + "</head>\n"
                        + "<body>\n"
                        + "    <h1>Welcome to MicroTomcat inside context</h1>\n"
                        + "    <p>Server is running successfully!</p>\n"
                        + "</body>\n"
                        + "</html>";
                    
                    byte[] contentBytes = content.getBytes("UTF-8");
                    wrappedResponse.setContentLength(contentBytes.length);
                    
                    // 先发送头部
                    response.sendHeaders();
                    
                    // 再发送内容
                    response.getOutputStream().write(contentBytes);
                    response.getOutputStream().flush();
                }
                return;
            }
            
            // 获取 Servlet 路径
            String servletPath = getServletPath(uri);
            
            // 查找匹配的 Wrapper（Servlet）
            Container wrapper = findChild(servletPath);
            
            if (wrapper != null && wrapper instanceof Wrapper) {
                // 使用原始的 response 对象调用 Wrapper
                ((Wrapper) wrapper).service(request, response);
            } else {
                // 处理静态资源
                String relativePath = uri.startsWith("/") ? uri.substring(1) : uri;
                File file = new File(docBase, relativePath);
                
                try {
                    if (file.exists() && file.isFile()) {
                        wrappedResponse.setContentType(getServletContext().getMimeType(uri));
                        // 设置内容长度
                        byte[] content = Files.readAllBytes(file.toPath());
                        wrappedResponse.setContentLength(content.length);
                        
                        // 使用 PrintWriter 而不是 OutputStream
                        PrintWriter writer = wrappedResponse.getWriter();
                        writer.write(new String(content, StandardCharsets.UTF_8));
                        writer.flush();
                    } else {
                        // 如果文件不存在，返回一个简单的默认页面
                        if (uri.equals("/index.html")) {
                            wrappedResponse.setContentType("text/html");
                            PrintWriter writer = wrappedResponse.getWriter();
                            writer.println("<html><body>");
                            writer.println("<h1>Welcome to MicroTomcat inside context</h1>");
                            writer.println("<p>Server is running successfully!</p>");
                            writer.println("</body></html>");
                        } else {
                            wrappedResponse.sendError(404, "File Not Found: " + uri);
                        }
                    }
                } catch (IOException e) {
                    log("Error sending response: " + e.getMessage());
                    wrappedResponse.sendError(500, "Internal Server Error");
                }
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
} 