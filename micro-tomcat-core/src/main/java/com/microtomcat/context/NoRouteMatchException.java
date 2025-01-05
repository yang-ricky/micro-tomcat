package com.microtomcat.context;

import javax.servlet.ServletException;

public class NoRouteMatchException extends ServletException {
    public NoRouteMatchException() {
        super("No route matches the request");
    }

    public NoRouteMatchException(String message) {
        super(message);
    }
} 