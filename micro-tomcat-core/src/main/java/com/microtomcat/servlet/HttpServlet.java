package com.microtomcat.servlet;

import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import javax.servlet.ServletException;
import java.io.IOException;

@Deprecated
public abstract class HttpServlet extends javax.servlet.http.HttpServlet {
    
    @Override
    public void init() throws ServletException {
        // Default implementation
    }

    // 提供一个重载的service方法，处理自定义的Request/Response
    public void service(Request request, Response response) 
            throws ServletException, IOException {
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

    // 提供重载的doGet方法，处理自定义的Request/Response
    protected void doGet(Request request, Response response) 
            throws ServletException, IOException {
        String errorMsg = "GET method not implemented";
        response.sendError(501, errorMsg);
    }

    // 提供重载的doPost方法，处理自定义的Request/Response
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