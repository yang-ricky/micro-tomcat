package com.microtomcat.pipeline.valve;

import com.microtomcat.pipeline.Valve;
import com.microtomcat.pipeline.ValveContext;
import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import com.microtomcat.servlet.ServletException;
import java.io.IOException;

public class StandardValve implements Valve {
    private final String webRoot;

    public StandardValve(String webRoot) {
        this.webRoot = webRoot;
    }

    @Override
    public void invoke(Request request, Response response, ValveContext context) 
            throws IOException, ServletException {
        // 由于现在使用了容器体系，这个基础阀门只需要传递请求
        context.invokeNext(request, response);
    }
} 