package com.microtomcat.container;

import com.microtomcat.lifecycle.LifecycleBase;
import com.microtomcat.pipeline.Pipeline;
import com.microtomcat.pipeline.StandardPipeline;
import java.util.concurrent.ConcurrentHashMap;


public abstract class ContainerBase extends LifecycleBase implements Container {
    protected Container parent = null;
    protected final ConcurrentHashMap<String, Container> children = new ConcurrentHashMap<>();
    protected Pipeline pipeline = new StandardPipeline();
    protected String name = null;

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
        log("Removed child container: " + child.getName());
    }

    protected void log(String message) {
        System.out.println("[" + getClass().getSimpleName() + "] " + message);
    }
} 