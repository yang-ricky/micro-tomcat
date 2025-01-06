package com.microtomcat.example;

import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import com.microtomcat.servlet.HttpServlet;
import javax.servlet.ServletException;
import java.io.IOException;

public class HelloServlet extends HttpServlet {
    @Override
    public void service(Request request, Response response) 
            throws ServletException, IOException {
        response.setContentType("text/html");
        response.getWriter().println("<h1>Hello from HelloServlet!</h1>");
    }
} 