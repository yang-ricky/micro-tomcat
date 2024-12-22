package com.microtomcat.container;

import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import com.microtomcat.lifecycle.LifecycleException;
import com.microtomcat.servlet.Servlet;
import com.microtomcat.servlet.ServletException;
import com.microtomcat.context.Context;
import java.io.IOException;

public class Wrapper extends ContainerBase {
    private Servlet servlet;
    private final String servletClass;

    public Wrapper(String name, String servletClass) {
        this.name = name;
        this.servletClass = servletClass;
    }

    @Override
    protected void initInternal() throws LifecycleException {
        log("Initializing Wrapper: " + name);
        try {
            Context context = (Context) getParent();
            Class<?> servletClass = context.getServletLoader().loadClass(this.servletClass);
            servlet = (Servlet) servletClass.getDeclaredConstructor().newInstance();
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
} 