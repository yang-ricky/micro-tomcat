package com.microtomcat.connector;

import javax.servlet.http.*;
import java.io.*;
import java.util.*;

public class ServletRequestWrapper extends HttpServletRequestWrapper {
    private final Request request;

    public ServletRequestWrapper(Request request) {
        super(request);
        this.request = request;
    }

    // 继承自 HttpServletRequest 的方法
    @Override
    public String getMethod() {
        return request.getMethod();
    }

    @Override
    public String getPathInfo() {
        return request.getPathInfo();
    }

    @Override
    public String getServletPath() {
        return request.getServletPath();
    }

    @Override
    public String getContextPath() {
        return request.getContextPath();
    }

    @Override
    public String getQueryString() {
        return request.getQueryString();
    }

    @Override
    public String getRequestURI() {
        return request.getRequestURI();
    }

    // 其他 HTTP 相关方法保持不变...
} 