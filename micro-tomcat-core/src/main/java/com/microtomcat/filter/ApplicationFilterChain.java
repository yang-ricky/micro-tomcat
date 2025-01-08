package com.microtomcat.filter;

import javax.servlet.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ApplicationFilterChain implements FilterChain {
    private final List<Filter> filters = new ArrayList<>();
    private int position = 0;
    private Servlet servlet;

    public void addFilter(Filter filter) {
        filters.add(filter);
    }

    public void setServlet(Servlet servlet) {
        this.servlet = servlet;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response) 
            throws IOException, ServletException {
        if (position < filters.size()) {
            Filter filter = filters.get(position++);
            filter.doFilter(request, response, this);
        } else if (servlet != null) {
            servlet.service(request, response);
        }
    }

    public void reset() {
        position = 0;
    }
} 