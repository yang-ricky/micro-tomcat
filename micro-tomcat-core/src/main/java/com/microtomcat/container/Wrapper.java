package com.microtomcat.container;

import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import com.microtomcat.lifecycle.LifecycleException;
import com.microtomcat.connector.ServletRequestWrapper;
import com.microtomcat.connector.ServletResponseWrapper;
import javax.servlet.*;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

public class Wrapper extends ContainerBase {
    private final String servletClass;
    private Servlet servlet;

    public Wrapper(String name, String servletClass) {
        this.name = name;
        this.servletClass = servletClass;
    }

    public void service(Request request, Response response) throws ServletException, IOException {
        if (servlet == null) {
            try {
                // 使用上下文的类加载器加载 servlet
                ClassLoader loader = getParent().getWebAppClassLoader();
                Class<?> clazz = loader.loadClass(servletClass);
                servlet = (Servlet) clazz.newInstance();
                
                // 初始化 servlet
                servlet.init(getServletConfig());
            } catch (Exception e) {
                throw new ServletException("Error initializing servlet", e);
            }
        }
        
        // 包装请求和响应
        ServletRequestWrapper servletRequest = new ServletRequestWrapper(request);
        ServletResponseWrapper servletResponse = new ServletResponseWrapper(response);
        
        // 调用 servlet 的 service 方法
        servlet.service(servletRequest, servletResponse);
        
        // 确保响应被提交
        if (!response.isCommitted()) {
            // 从包装器中获取设置的 Content-Type
            String contentType = servletResponse.getContentType();
            log("Content-Type from wrapper: " + contentType);
            log("Original response Content-Type: " + response.getContentType());
            
            if (contentType != null) {
                response.setContentType(contentType);
                log("After setting Content-Type: " + response.getContentType());
            }
            response.flushBuffer();
        }
    }

    public void setServlet(Servlet servlet) {
        this.servlet = servlet;
    }

    private ServletConfig getServletConfig() {
        // 返回一个基本的 ServletConfig 实现
        return new ServletConfig() {
            @Override
            public String getServletName() {
                return getName();
            }

            @Override
            public ServletContext getServletContext() {
                return ((Context) getParent()).getServletContext();
            }

            @Override
            public String getInitParameter(String name) {
                return null;
            }

            @Override
            public Enumeration<String> getInitParameterNames() {
                return Collections.emptyEnumeration();
            }
        };
    }

    @Override
    protected void destroyInternal() throws LifecycleException {
        try {
            if (servlet != null) {
                servlet.destroy();
            }
        } finally {
            servlet = null;
        }
    }

    @Override
    protected void stopInternal() throws LifecycleException {
        try {
            if (servlet != null) {
                servlet.destroy();
                servlet = null;
            }
        } catch (Exception e) {
            throw new LifecycleException("Error stopping servlet", e);
        }
    }

    @Override
    protected void startInternal() throws LifecycleException {
        try {
            // 在启动时初始化 servlet
            if (servlet == null) {
                ClassLoader loader = getParent().getWebAppClassLoader();
                Class<?> clazz = loader.loadClass(servletClass);
                servlet = (Servlet) clazz.newInstance();
                servlet.init(getServletConfig());
            }
        } catch (Exception e) {
            throw new LifecycleException("Error starting servlet", e);
        }
    }

    @Override
    protected void initInternal() throws LifecycleException {
        try {
            // 在初始化阶段，我们可以进行一些准备工作
            // 但实际的 servlet 初始化会在 startInternal 中进行
            if (servletClass == null) {
                throw new LifecycleException("No servlet class configured");
            }
            
            // 验证父容器是否是 Context
            if (!(getParent() instanceof Context)) {
                throw new LifecycleException("Wrapper container must have Context as parent");
            }
            
        } catch (Exception e) {
            throw new LifecycleException("Error initializing wrapper", e);
        }
    }

    @Override
    public void invoke(Request request, Response response) {
        try {
            // 调用 service 方法处理请求
            service(request, response);
            
            // 确保响应被提交
            if (!response.isCommitted()) {
                response.flushBuffer();
            }
        } catch (ServletException | IOException e) {
            // 记录错误并可能设置错误响应
            log("Error processing request: " + e.getMessage());
            try {
                response.sendError(500, "Internal Server Error");
            } catch (IOException ex) {
                log("Failed to send error response: " + ex.getMessage());
            }
        }
    }

    public Servlet getServlet() {
        return servlet;
    }
} 