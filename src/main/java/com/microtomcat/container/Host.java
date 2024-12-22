package com.microtomcat.container;

import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import com.microtomcat.lifecycle.LifecycleException;
import com.microtomcat.context.Context;
import java.io.IOException;

public class Host extends ContainerBase {
    private String appBase = "webapps";

    public Host(String name) {
        this.name = name;
    }

    public String getAppBase() {
        return appBase;
    }

    public void setAppBase(String appBase) {
        this.appBase = appBase;
    }

    @Override
    public void invoke(Request request, Response response) {
        String contextPath = getContextPath(request.getUri());
        Context context = (Context) findChild(contextPath);
        
        if (context != null) {
            context.invoke(request, response);
        } else {
            log("No matching context found for: " + contextPath);
            try {
                response.sendError(404, "No matching context found for: " + contextPath);
            } catch (IOException e) {
                log("Error sending 404 response: " + e.getMessage());
            }
        }
    }

    private String getContextPath(String uri) {
        String contextPath = "/";
        if (uri != null && uri.length() > 0) {
            int secondSlash = uri.indexOf('/', 1);
            if (secondSlash != -1) {
                contextPath = uri.substring(0, secondSlash);
            } else {
                contextPath = uri;
            }
        }
        return contextPath;
    }

    @Override
    protected void initInternal() throws LifecycleException {
        log("Initializing Host: " + name);
    }

    @Override
    protected void startInternal() throws LifecycleException {
        log("Starting Host: " + name);
        Container[] children = findChildren();
        for (Container child : children) {
            child.start();
        }
    }

    @Override
    protected void stopInternal() throws LifecycleException {
        log("Stopping Host: " + name);
        Container[] children = findChildren();
        for (Container child : children) {
            child.stop();
        }
    }

    @Override
    protected void destroyInternal() throws LifecycleException {
        log("Destroying Host: " + name);
    }
} 