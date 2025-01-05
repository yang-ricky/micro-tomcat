package com.microtomcat.servlet;

import org.junit.Test;
import javax.servlet.*;

public class ServletAvailabilityTest {

    @Test
    public void testServletAPIAvailability() {
        Class<?>[] requiredClasses = {
            ServletException.class,
            Servlet.class,
            ServletRequest.class,
            ServletResponse.class,
            ServletContext.class,
            Filter.class
        };

        for (Class<?> clazz : requiredClasses) {
            try {
                Class.forName(clazz.getName());
            } catch (ClassNotFoundException e) {
                throw new AssertionError(
                    String.format("Required Servlet API class %s is not available", clazz.getName()), 
                    e
                );
            }
        }
    }
} 