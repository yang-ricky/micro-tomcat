package com.microtomcat.pipeline;

import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import java.io.IOException;
import com.microtomcat.servlet.ServletException;

public interface Valve {
    void invoke(Request request, Response response, ValveContext context) 
        throws IOException, ServletException;
} 