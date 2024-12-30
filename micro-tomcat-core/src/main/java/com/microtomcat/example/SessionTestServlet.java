package com.microtomcat.example;

import com.microtomcat.servlet.HttpServlet;
import com.microtomcat.servlet.ServletException;
import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import com.microtomcat.session.Session;
import java.io.IOException;

public class SessionTestServlet extends HttpServlet {
    @Override
    protected void doGet(Request request, Response response) 
            throws ServletException, IOException {
        Session session = request.getSession();
        Integer visitCount = (Integer) session.getAttribute("visitCount");
        
        if (visitCount == null) {
            visitCount = 1;
        } else {
            visitCount++;
        }
        
        session.setAttribute("visitCount", visitCount);
        
        String content = String.format(
            "<html><body>" +
            "<h1>Session Test</h1>" +
            "<p>Session ID: %s</p>" +
            "<p>Visit Count: %d</p>" +
            "<p>Session Creation Time: %s</p>" +
            "</body></html>",
            session.getId(),
            visitCount,
            session.getCreationTime()
        );
        
        response.sendServletResponse(content);
    }
} 