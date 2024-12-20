package com.microtomcat.example;

import com.microtomcat.servlet.HttpServlet;
import com.microtomcat.servlet.ServletException;
import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import java.io.IOException;

public class HelloServlet extends HttpServlet {
    @Override
    protected void doGet(Request request, Response response) 
            throws ServletException, IOException {
        String content = "<html><body><h1>Hello from HelloServlet!</h1></body></html>";
        response.sendServletResponse(content);
    }
} 