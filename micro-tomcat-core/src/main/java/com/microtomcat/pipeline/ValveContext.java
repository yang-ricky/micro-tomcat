package com.microtomcat.pipeline;

import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import java.io.IOException;
import com.microtomcat.servlet.ServletException;

public interface ValveContext {
    void invokeNext(Request request, Response response) 
        throws IOException, ServletException;
} 