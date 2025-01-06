package com.microtomcat.servlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;

public class MockDispatcherServlet extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        String uri = req.getRequestURI();
        String method = req.getMethod();
        
        if ("/test/hello".equals(uri) && "GET".equals(method)) {
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("Hello from TestController");
        } else if ("GET".equals(method)) {
            resp.sendError(404, "Not Found");
        } else {
            resp.sendError(405, "Method Not Allowed");
        }
    }
} 