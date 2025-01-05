package com.microtomcat.connector;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

public class ServletResponseWrapper implements ServletResponse {
    private final Response response;

    public ServletResponseWrapper(Response response) {
        this.response = response;
    }

    @Override
    public void setContentType(String type) {
        response.setContentType(type);
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return response.getWriter();
    }

    // 实现其他必要的抽象方法
    @Override
    public String getCharacterEncoding() {
        return "UTF-8";
    }

    @Override
    public String getContentType() {
        return response.getContentType();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException("getOutputStream not implemented");
    }

    @Override
    public void setCharacterEncoding(String charset) {
        // 暂时不实现
    }

    @Override
    public void setContentLength(int len) {
        response.setContentLength(len);
    }

    @Override
    public void setBufferSize(int size) {
        // 暂时不实现
    }

    @Override
    public int getBufferSize() {
        return 0;
    }

    @Override
    public void flushBuffer() throws IOException {
        // 暂时不实现
    }

    @Override
    public void resetBuffer() {
        // 暂时不实现
    }

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void reset() {
        // 暂时不实现
    }

    @Override
    public void setLocale(Locale loc) {
        // 暂时不实现
    }

    @Override
    public Locale getLocale() {
        return Locale.getDefault();
    }

    @Override
    public void setContentLengthLong(long length) {
        setContentLength((int) length);
    }
} 