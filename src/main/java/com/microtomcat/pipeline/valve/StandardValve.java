package com.microtomcat.pipeline.valve;

import com.microtomcat.pipeline.Valve;
import com.microtomcat.pipeline.ValveContext;
import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import com.microtomcat.servlet.Servlet;
import com.microtomcat.servlet.ServletLoader;
import java.io.IOException;
import com.microtomcat.servlet.ServletException;

public class StandardValve implements Valve {
    private final String webRoot;
    private final ServletLoader servletLoader;

    public StandardValve(String webRoot, ServletLoader servletLoader) {
        this.webRoot = webRoot;
        this.servletLoader = servletLoader;
    }

    @Override
    public void invoke(Request request, Response response, ValveContext context) 
            throws IOException, ServletException {
        System.out.println("[StandardValve] Processing request for URI: " + request.getUri());
        
        String uri = request.getUri();
        
        // 检查是否是 Servlet 请求
        if (uri.startsWith("/servlet/")) {
            System.out.println("StandardValve: Processing Servlet request");
            // 处理 Servlet 请求
            Servlet servlet = servletLoader.loadServlet(uri);
            servlet.service(request, response);
        } else {
            System.out.println("StandardValve: Processing static resource request");
            // 处理静态资源请求
            response.sendStaticResource();
        }
        
        // 由于这是基础阀门，通常是管道的最后一个阀门，
        // 所以不需要调用 context.invokeNext()
    }
} 