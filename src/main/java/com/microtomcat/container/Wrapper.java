package com.microtomcat.container;

import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import com.microtomcat.lifecycle.LifecycleException;
import com.microtomcat.servlet.Servlet;
import com.microtomcat.servlet.ServletException;
import com.microtomcat.context.Context;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.ServletRequestListener;

public class Wrapper extends ContainerBase {
    private Servlet servlet;
    private final String servletClass;
    private Map<String, String> initParameters = new HashMap<>();
    private List<Filter> filterChain = new ArrayList<>();
    private List<ServletRequestListener> requestListeners = new ArrayList<>();

    public Wrapper(String name, String servletClass) {
        this.name = name;
        this.servletClass = servletClass;
    }

    @Override
    protected void initInternal() throws LifecycleException {
        try {
            Context context = (Context) getParent();
            Class<?> servletClass;
            
            // 1. 如果是框架提供的 Servlet (com.microtomcat.example.*)，用系统类加载器
            if (this.servletClass.startsWith("com.microtomcat.")) {
                ClassLoader loader = getParent().getClass().getClassLoader();
                servletClass = loader.loadClass(this.servletClass);
            } 
            // 2. 如果是应用的 Servlet，用对应的 WebAppClassLoader
            else {
                ClassLoader loader = context.getWebAppClassLoader();
                log("Loading servlet class: " + this.servletClass + " using loader: " + loader);
                servletClass = loader.loadClass(this.servletClass);
            }

            // 确保它实现了 Servlet 接口
            if (!com.microtomcat.servlet.Servlet.class.isAssignableFrom(servletClass)) {
                throw new LifecycleException("Class " + servletClass + " is not a Servlet");
            }
            
            servlet = (com.microtomcat.servlet.Servlet) servletClass.getDeclaredConstructor().newInstance();
            servlet.init();
            
        } catch (Exception e) {
            throw new LifecycleException("Failed to initialize servlet: " + name, e);
        }
    }

    @Override
    public void invoke(Request request, Response response) {
        try {
            if (servlet != null) {
                servlet.service(request, response);
            } else {
                throw new ServletException("No servlet instance available");
            }
        } catch (ServletException | IOException e) {
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
            if (servlet == null && servletClass != null) {
                Class<?> clazz = Class.forName(servletClass);
                servlet = (Servlet) clazz.getDeclaredConstructor().newInstance();
                servlet.init();
            }
        } catch (Exception e) {
            throw new LifecycleException("Failed to initialize servlet", e);
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