package com.microtomcat.container;

import com.microtomcat.lifecycle.Lifecycle;
import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import com.microtomcat.pipeline.Pipeline;

public interface Container extends Lifecycle {
    public Container getParent();
    public void setParent(Container container);
    
    public String getName();
    public void setName(String name);
    
    public Pipeline getPipeline();
    
    public void addChild(Container child);
    public Container findChild(String name);
    public Container[] findChildren();
    public void removeChild(Container child);
    
    public void invoke(Request request, Response response);
} 