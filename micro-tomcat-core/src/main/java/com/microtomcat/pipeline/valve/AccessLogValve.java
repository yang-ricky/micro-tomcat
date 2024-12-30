package com.microtomcat.pipeline.valve;

import com.microtomcat.pipeline.Valve;
import com.microtomcat.pipeline.ValveContext;
import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.microtomcat.servlet.ServletException;
public class AccessLogValve implements Valve {
    private static final DateTimeFormatter formatter = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void invoke(Request request, Response response, ValveContext context) 
            throws IOException, ServletException {
        String logMessage = String.format("[AccessLogValve][%s] %s %s %s",
            LocalDateTime.now().format(formatter),
            request.getMethod(),
            request.getUri(),
            request.getProtocol());
        
        System.out.println(logMessage);
        
        context.invokeNext(request, response);
    }
} 