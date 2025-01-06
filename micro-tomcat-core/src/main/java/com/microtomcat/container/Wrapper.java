package com.microtomcat.container;

import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import com.microtomcat.lifecycle.LifecycleException;
import com.microtomcat.context.Context;
import com.microtomcat.connector.ServletRequestWrapper;
import com.microtomcat.connector.ServletResponseWrapper;
import javax.servlet.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Wrapper extends ContainerBase {
    private javax.servlet.Servlet servlet;
    private final String servletClass;
    private Map<String, String> initParameters = new HashMap<>();
    private List<Filter> filterChain = new ArrayList<>();
    private List<ServletRequestListener> requestListeners = new ArrayList<>();

    public Wrapper(String name, String servletClass) {
        this.name = name;
        this.servletClass = servletClass;
    }

    public void setServlet(javax.servlet.Servlet servlet) {
        this.servlet = servlet;
    }

    @Override
    protected void initInternal() throws LifecycleException {
        try {
            Context context = (Context) getParent();
            Class<?> servletClass;
            
            if (this.servletClass.startsWith("com.microtomcat.")) {
                ClassLoader loader = getParent().getClass().getClassLoader();
                servletClass = loader.loadClass(this.servletClass);
            } else {
                ClassLoader loader = context.getWebAppClassLoader();
                servletClass = loader.loadClass(this.servletClass);
            }

            // 检查是否实现了标准Servlet接口
            if (!javax.servlet.Servlet.class.isAssignableFrom(servletClass)) {
                throw new LifecycleException("Class " + servletClass + " is not a Servlet");
            }
            
            servlet = (javax.servlet.Servlet) servletClass.getDeclaredConstructor().newInstance();
            servlet.init(createServletConfig());
            
        } catch (Exception e) {
            throw new LifecycleException("Failed to initialize servlet: " + name, e);
        }
    }

    private javax.servlet.ServletConfig createServletConfig() {
        return new javax.servlet.ServletConfig() {
            @Override
            public String getServletName() {
                return name;
            }

            @Override
            public javax.servlet.ServletContext getServletContext() {
                return null; // TODO: 实现ServletContext
            }

            @Override
            public String getInitParameter(String name) {
                return initParameters.get(name);
            }

            @Override
            public java.util.Enumeration<String> getInitParameterNames() {
                return java.util.Collections.enumeration(initParameters.keySet());
            }
        };
    }

    @Override
    public void invoke(Request request, Response response) {
        try {
            if (servlet != null) {
                // 使用包装器将我们的 Request/Response 转换为标准的 ServletRequest/ServletResponse
                ServletRequest servletRequest = new ServletRequestWrapper(request);
                ServletResponse servletResponse = new ServletResponseWrapper(response);
                servlet.service(servletRequest, servletResponse);
            } else {
                throw new ServletException("No servlet instance available");
            }
        } catch (Exception e) {
            log("Error invoking servlet: " + e.getMessage());
            try {
                response.sendError(500, "Internal Server Error: " + e.getMessage());
            } catch (IOException ioe) {
                log("Failed to send error response: " + ioe.getMessage());
            }
        }
    }

    @Override
    protected void startInternal() throws LifecycleException {
        log("Starting Wrapper: " + name);
        try {
            if (servlet == null) {
                initInternal();
            }
        } catch (Exception e) {
            throw new LifecycleException("Failed to start wrapper", e);
        }
    }

    @Override
    protected void stopInternal() throws LifecycleException {
        log("Stopping Wrapper: " + name);
        if (servlet != null) {
            servlet.destroy();
            servlet = null;
        }
    }

    @Override
    protected void destroyInternal() throws LifecycleException {
        log("Destroying Wrapper: " + name);
        if (servlet != null) {
            servlet.destroy();
            servlet = null;
        }
    }

    public void addInitParameter(String name, String value) {
        initParameters.put(name, value);
    }

    public void addFilter(Filter filter) {
        filterChain.add(filter);
    }

    public void addRequestListener(ServletRequestListener listener) {
        requestListeners.add(listener);
    }
} 