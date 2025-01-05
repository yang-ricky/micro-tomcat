package com.microtomcat.connector;

import javax.servlet.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

public class ServletRequestWrapper implements ServletRequest {
    private final Request request;

    public ServletRequestWrapper(Request request) {
        this.request = request;
    }

    @Override
    public String getParameter(String name) {
        return request.getParameter(name);
    }

    @Override
    public String getProtocol() {
        return request.getProtocol();
    }

    // 实现其他必要的抽象方法
    @Override
    public Object getAttribute(String name) {
        return request.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return request.getAttributeNames();
    }

    @Override
    public String getCharacterEncoding() {
        return "UTF-8";
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        // 暂时不实现
    }

    @Override
    public int getContentLength() {
        return request.getContentLength();
    }

    @Override
    public String getContentType() {
        return request.getContentType();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        throw new UnsupportedOperationException("getInputStream not implemented");
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return request.getParameterNames();
    }

    @Override
    public String[] getParameterValues(String name) {
        return request.getParameterValues(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return request.getParameterMap();
    }

    @Override
    public String getServerName() {
        return request.getServerName();
    }

    @Override
    public int getServerPort() {
        return request.getServerPort();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        throw new UnsupportedOperationException("getReader not implemented");
    }

    @Override
    public String getRemoteAddr() {
        return request.getRemoteAddr();
    }

    @Override
    public String getRemoteHost() {
        return request.getRemoteHost();
    }

    @Override
    public void setAttribute(String name, Object o) {
        request.setAttribute(name, o);
    }

    @Override
    public void removeAttribute(String name) {
        request.removeAttribute(name);
    }

    @Override
    public Locale getLocale() {
        return Locale.getDefault();
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return null;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    @Override
    public String getRealPath(String path) {
        return null;
    }

    @Override
    public int getRemotePort() {
        return request.getRemotePort();
    }

    @Override
    public String getLocalName() {
        return request.getLocalName();
    }

    @Override
    public String getLocalAddr() {
        return request.getLocalAddr();
    }

    @Override
    public int getLocalPort() {
        return request.getLocalPort();
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        throw new UnsupportedOperationException("Async not supported");
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        throw new UnsupportedOperationException("Async not supported");
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        return null;
    }

    @Override
    public DispatcherType getDispatcherType() {
        return DispatcherType.REQUEST;
    }

    @Override
    public String getScheme() {
        return request.getScheme();
    }

    @Override
    public long getContentLengthLong() {
        return getContentLength();
    }
} 