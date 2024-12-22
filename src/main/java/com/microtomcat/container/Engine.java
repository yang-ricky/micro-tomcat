package com.microtomcat.container;

import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import com.microtomcat.lifecycle.LifecycleException;
import java.io.IOException;

public class Engine extends ContainerBase {
    private String defaultHost;

    public Engine(String name, String defaultHost) {
        this.name = name;
        this.defaultHost = defaultHost;
    }

    public String getDefaultHost() {
        return defaultHost;
    }

    public void setDefaultHost(String defaultHost) {
        this.defaultHost = defaultHost;
    }

    @Override
    public void invoke(Request request, Response response) {
        String hostHeader = request.getHeader("Host");
        String hostName = hostHeader != null ? hostHeader.split(":")[0] : defaultHost;
        
        Host host = (Host) findChild(hostName);
        
        if (host == null) {
            host = (Host) findChild(defaultHost);
        }
        
        if (host != null) {
            host.invoke(request, response);
        } else {
            log("No matching host found for: " + hostName);
            try {
                response.sendError(404, "No matching host found");
            } catch (IOException e) {
                log("Error sending 404 response: " + e.getMessage());
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
} 