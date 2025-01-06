package com.microtomcat.example;

import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import com.microtomcat.servlet.HttpServlet;
import javax.servlet.ServletException;
import java.io.IOException;
import com.microtomcat.session.Session;

public class SessionTestServlet extends HttpServlet {
    @Override
    protected void doGet(Request request, Response response)
            throws javax.servlet.ServletException, IOException {
        Session session = request.getSession(true);
        Integer count = (Integer) session.getAttribute("count");
        if (count == null) {
            count = 1;
        } else {
            count++;
        }
        session.setAttribute("count", count);
        
        response.setContentType("text/html");
        response.getWriter().println("<h1>Session Test</h1>");
        response.getWriter().println("<p>Session ID: " + session.getId() + "</p>");
        response.getWriter().println("<p>Visit count: " + count + "</p>");
    }
} 