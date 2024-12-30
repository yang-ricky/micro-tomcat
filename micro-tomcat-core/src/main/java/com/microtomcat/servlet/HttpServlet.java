package com.microtomcat.servlet;

import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import java.io.IOException;

public abstract class HttpServlet implements Servlet {
    
    @Override
    public void init() throws ServletException {
        // Default implementation
    }

    @Override
    public void service(Request request, Response response) throws ServletException, IOException {
        String method = request.getMethod();
        
        if (method.equals("GET")) {
            doGet(request, response);
        } else if (method.equals("POST")) {
            doPost(request, response);
        } else {
            String errorMsg = "Method " + method + " not implemented";
            response.sendError(501, errorMsg);
        }
    }

    protected void doGet(Request request, Response response) 
            throws ServletException, IOException {
        String errorMsg = "GET method not implemented";
        response.sendError(501, errorMsg);
    }

    protected void doPost(Request request, Response response) 
            throws ServletException, IOException {
        String errorMsg = "POST method not implemented";
        response.sendError(501, errorMsg);
    }

    @Override
    public void destroy() {
        // Default implementation
    }
} 