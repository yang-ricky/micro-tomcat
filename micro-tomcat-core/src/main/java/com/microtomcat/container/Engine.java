package com.microtomcat.container;

import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import com.microtomcat.lifecycle.LifecycleException;
import java.io.IOException;
import javax.servlet.ServletContext;
import com.microtomcat.context.SimpleServletContext;

public class Engine extends ContainerBase {
    private String defaultHost;
    private ServletContext servletContext;

    public Engine(String name, String defaultHost) {
        this.name = name;
        this.defaultHost = defaultHost;
    }

    public String getDefaultHost() {
        return defaultHost;
    }

    public Host getDefaultHostContainer() {
        return (Host) findChild(defaultHost);
    }

    public void setDefaultHost(String defaultHost) {
        this.defaultHost = defaultHost;
    }

    @Override
    public void invoke(Request request, Response response) {
        String hostName = request.getServerName();
        if (hostName == null) {
            hostName = defaultHost;
        }
        
        Host host = (Host) findChild(hostName);
        if (host == null) {
            host = (Host) findChild(defaultHost);
        }
        
        if (host != null) {
            host.invoke(request, response);
        } else {
            try {
                response.sendError(400, "Invalid virtual host: " + hostName);
            } catch (IOException e) {
                log("Error sending 400 response: " + e.getMessage());
            }
        }
    }

    @Override
    protected void initInternal() throws LifecycleException {
        log("Initializing Engine: " + name);
    }

    @Override
    protected void startInternal() throws LifecycleException {
        log("Starting Engine: " + name);
        Container[] children = findChildren();
        for (Container child : children) {
            child.start();
        }
    }

    @Override
    protected void stopInternal() throws LifecycleException {
        log("Stopping Engine: " + name);
        Container[] children = findChildren();
        for (Container child : children) {
            child.stop();
        }
    }

    @Override
    protected void destroyInternal() throws LifecycleException {
        log("Destroying Engine: " + name);
    }

    public ServletContext getServletContext() {
        if (servletContext == null) {
            // 创建一个新的 ServletContext，使用 Engine 名称作为上下文路径
            servletContext = new SimpleServletContext(getName());
        }
        return servletContext;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }
} 