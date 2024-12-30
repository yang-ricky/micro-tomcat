package com.microtomcat.example;

import com.microtomcat.servlet.HttpServlet;
import com.microtomcat.servlet.ServletException;
import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import java.io.IOException;

public class HelloServlet extends HttpServlet {
    @Override
    public void service(Request request, Response response) throws ServletException, IOException {
        response.sendServletResponse("Hello from HelloServlet!");
    }
} 