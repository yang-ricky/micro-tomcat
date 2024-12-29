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
        
        if (context == null) {
            context = (Context) findChild("");
        }
        
        if (context != null) {
            context.invoke(request, response);
        } else {
            try {
                response.sendError(404, "Context not found for: " + request.getUri());
            } catch (IOException e) {
                log("Error sending 404 response: " + e.getMessage());
            }
        }
    }

    private String getContextPath(String uri) {
        if (uri == null) return "";
        int nextSlash = uri.indexOf('/', 1);
        if (nextSlash != -1) {
            return uri.substring(0, nextSlash);
        }
        return uri;
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

    public Context getDefaultContext() {
        return (Context) findChild("");
    }
} 