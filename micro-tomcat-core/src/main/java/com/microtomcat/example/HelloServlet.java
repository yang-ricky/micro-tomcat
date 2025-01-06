package com.microtomcat.example;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

public class HelloServlet extends HttpServlet {
    @Override
    public void service(ServletRequest req, ServletResponse res) 
            throws ServletException, IOException {
        res.setContentType("text/html;charset=UTF-8");
        res.getWriter().write("<html><body>");
        res.getWriter().write("<h1>Hello from HelloServlet!</h1>");
        res.getWriter().write("</body></html>");
        res.getWriter().flush();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write("<html><body>");
        response.getWriter().write("<h1>Hello from HelloServlet!</h1>");
        response.getWriter().write("</body></html>");
    }
} 