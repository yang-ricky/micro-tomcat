package com.microtomcat.filter;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

public class ApplicationFilterConfig implements FilterConfig {
    private final String filterName;
    private final ServletContext servletContext;
    private final Map<String, String> initParameters = new HashMap<>();

    public ApplicationFilterConfig(String filterName, ServletContext servletContext) {
        this.filterName = filterName;
        this.servletContext = servletContext;
    }

    @Override
    public String getFilterName() {
        return filterName;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public String getInitParameter(String name) {
        return initParameters.get(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(initParameters.keySet());
    }

    public void addInitParameter(String name, String value) {
        initParameters.put(name, value);
    }
} 