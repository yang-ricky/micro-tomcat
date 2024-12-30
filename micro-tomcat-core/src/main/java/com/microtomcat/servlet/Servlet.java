package com.microtomcat.servlet;

import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import java.io.IOException;

public interface Servlet {
    void init() throws ServletException;
    void service(Request request, Response response) throws ServletException, IOException;
    void destroy();
}