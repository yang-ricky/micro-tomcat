package com.microtomcat.pipeline.valve;

import com.microtomcat.pipeline.Valve;
import com.microtomcat.pipeline.ValveContext;
import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import com.microtomcat.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

public class AuthenticatorValve implements Valve {
    @Override
    public void invoke(Request request, Response response, ValveContext context) 
            throws IOException, ServletException {
        System.out.println("[AuthenticatorValve] Processing request ..");
        
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object user = session.getAttribute("user");
            if (user != null) {
                context.invokeNext(request, response);
                return;
            }
        }
        
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
    }
} 