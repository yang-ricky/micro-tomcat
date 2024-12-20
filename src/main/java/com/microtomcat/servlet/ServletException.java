package com.microtomcat.servlet;

public class ServletException extends Exception {
    public ServletException(String message) {
        super(message);
    }

    public ServletException(String message, Throwable cause) {
        super(message, cause);
    }
} 