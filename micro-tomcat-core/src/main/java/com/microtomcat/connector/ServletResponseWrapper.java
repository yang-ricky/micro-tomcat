package com.microtomcat.connector;

import javax.servlet.http.*;
import javax.servlet.*;
import java.io.*;
import java.util.*;

public class ServletResponseWrapper extends HttpServletResponseWrapper {
    private final Response response;
    private boolean statusSet = false;
    private boolean contentLengthSet = false;

    public ServletResponseWrapper(Response response) {
        super(response);
        this.response = response;
    }

    @Override
    public void setStatus(int sc) {
        if (!isCommitted() && !statusSet) {
            response.setStatus(sc);
            statusSet = true;
        }
    }

    @Override
    public void setHeader(String name, String value) {
        if (!isCommitted()) {
            System.out.println("ServletResponseWrapper DEBUG: Setting header: " + name + " = " + value);
            if ("Content-Length".equalsIgnoreCase(name)) {
                setContentLength(Integer.parseInt(value));
            } else {
                response.setHeader(name, value);
            }
        }
    }

    @Override
    public void addHeader(String name, String value) {
        response.addHeader(name, value);
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        response.sendError(sc, msg);
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return response.getWriter();
    }

    @Override
    public String getCharacterEncoding() {
        return response.getCharacterEncoding();
    }

    @Override
    public String getContentType() {
        return response.getContentType();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return response.getOutputStream();
    }

    @Override
    public void setCharacterEncoding(String charset) {
        response.setCharacterEncoding(charset);
    }

    @Override
    public void setContentLength(int len) {
        if (!isCommitted() && !contentLengthSet) {
            System.out.println("ServletResponseWrapper DEBUG: Setting Content-Length: " + len);
            response.setContentLength(len);
            response.setHeader("Content-Length", String.valueOf(len));
            contentLengthSet = true;
        }
    }

    @Override
    public void setBufferSize(int size) {
        response.setBufferSize(size);
    }

    @Override
    public int getBufferSize() {
        return response.getBufferSize();
    }

    @Override
    public void flushBuffer() throws IOException {
        response.flushBuffer();
    }

    @Override
    public void resetBuffer() {
        response.resetBuffer();
    }

    @Override
    public boolean isCommitted() {
        return response.isCommitted();
    }

    @Override
    public void reset() {
        response.reset();
    }

    @Override
    public void setLocale(Locale loc) {
        response.setLocale(loc);
    }

    @Override
    public Locale getLocale() {
        return response.getLocale();
    }

    @Override
    public void setContentLengthLong(long length) {
        response.setContentLengthLong(length);
    }

    @Override
    public void setContentType(String type) {
        if (!isCommitted()) {
            response.setContentType(type);
        }
    }
} 