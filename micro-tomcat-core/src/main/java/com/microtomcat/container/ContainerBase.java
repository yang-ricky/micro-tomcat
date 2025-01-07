package com.microtomcat.container;

import com.microtomcat.lifecycle.LifecycleBase;
import com.microtomcat.pipeline.Pipeline;
import com.microtomcat.pipeline.StandardPipeline;
import java.util.concurrent.ConcurrentHashMap;
import com.microtomcat.container.event.ContainerEvent;
import com.microtomcat.container.event.ContainerListener;
import java.util.ArrayList;
import java.util.List;
import com.microtomcat.loader.WebAppClassLoader;

public abstract class ContainerBase extends LifecycleBase implements Container {
    protected Container parent = null;
    protected final ConcurrentHashMap<String, Container> children = new ConcurrentHashMap<>();
    protected Pipeline pipeline = new StandardPipeline();
    protected String name = null;
    private final List<ContainerListener> listeners = new ArrayList<>();

    @Override
    public Container getParent() { return parent; }

    @Override
    public void setParent(Container container) { 
        this.parent = container;
    }

    @Override
    public String getName() { return name; }

    @Override
    public void setName(String name) { this.name = name; }

    @Override
    public Pipeline getPipeline() { return pipeline; }

    @Override
    public void addChild(Container child) {
        child.setParent(this);
        children.put(child.getName(), child);
        fireContainerEvent(ContainerEvent.CHILD_ADDED, child);
        log("Added child container: " + child.getName());
    }

    @Override
    public Container findChild(String name) {
        return children.get(name);
    }

    @Override
    public Container[] findChildren() {
        return children.values().toArray(new Container[0]);
    }

    @Override
    public void removeChild(Container child) {
        children.remove(child.getName());
        fireContainerEvent(ContainerEvent.CHILD_REMOVED, child);
        log("Removed child container: " + child.getName());
    }

    public void addContainerListener(ContainerListener listener) {
        listeners.add(listener);
    }

    public void removeContainerListener(ContainerListener listener) {
        listeners.remove(listener);
    }

    protected void fireContainerEvent(String type, Object data) {
        ContainerEvent event = new ContainerEvent(this, type, data);
        for (ContainerListener listener : listeners) {
            listener.containerEvent(event);
        }
    }

    protected void log(String message) {
        System.out.println("[" + getClass().getSimpleName() + "] " + message);
    }

    @Override
    public WebAppClassLoader getWebAppClassLoader() {
        if (getParent() != null) {
            return getParent().getWebAppClassLoader();
        }
        return null;
    }
} 