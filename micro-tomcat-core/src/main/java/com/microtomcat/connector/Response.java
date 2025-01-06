package com.microtomcat.connector;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.*;
import java.nio.file.Files;
import java.io.File;

public class Response implements HttpServletResponse {
    private final OutputStream output;
    private PrintWriter writer;
    private String contentType;
    private String characterEncoding = "UTF-8";
    private int contentLength = -1;
    private int status = 200;
    private String statusMessage = "OK";
    private final Map<String, List<String>> headers = new HashMap<>();
    private boolean committed = false;

    public Response(OutputStream output) {
        this.output = output;
    }

    @Override
    public void setContentType(String type) {
        this.contentType = type;
        setHeader("Content-Type", type);
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (writer == null) {
            // 写入 HTTP 响应头
            output.write(("HTTP/1.1 " + status + " " + statusMessage + "\r\n").getBytes());
            
            // 写入所有响应头
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                for (String value : entry.getValue()) {
                    output.write((entry.getKey() + ": " + value + "\r\n").getBytes());
                }
            }
            
            output.write("\r\n".getBytes());  // 空行分隔头部和正文
            writer = new PrintWriter(output, true);
            committed = true;
        }
        return writer;
    }

    @Override
    public void setContentLength(int len) {
        this.contentLength = len;
        setHeader("Content-Length", String.valueOf(len));
    }

    @Override
    public void setHeader(String name, String value) {
        headers.put(name, Collections.singletonList(value));
    }

    @Override
    public void addHeader(String name, String value) {
        headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return headers.getOrDefault(name, Collections.emptyList());
    }

    @Override
    public Collection<String> getHeaderNames() {
        return headers.keySet();
    }

    @Override
    public String getHeader(String name) {
        List<String> values = headers.get(name);
        return values != null && !values.isEmpty() ? values.get(0) : null;
    }

    // 其他必要的实现方法
    @Override
    public void setStatus(int sc) {
        this.status = sc;
        switch (sc) {
            case 200: this.statusMessage = "OK"; break;
            case 404: this.statusMessage = "Not Found"; break;
            case 500: this.statusMessage = "Internal Server Error"; break;
            default: this.statusMessage = ""; break;
        }
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException("getOutputStream not implemented");
    }

    // 添加一个用于发送静态资源的方法
    public void sendStaticResource(File resource) throws IOException {
        if (resource.exists()) {
            setContentLength((int) resource.length());
            setContentType(getContentTypeByExtension(resource.getName()));
            Files.copy(resource.toPath(), output);
        } else {
            setStatus(404);
            getWriter().println("404 File Not Found");
        }
    }

    private String getContentTypeByExtension(String fileName) {
        if (fileName.endsWith(".html")) return "text/html";
        if (fileName.endsWith(".css")) return "text/css";
        if (fileName.endsWith(".js")) return "application/javascript";
        return "text/plain";
    }

    // 实现其他必要的 HttpServletResponse 方法
    @Override
    public void addCookie(Cookie cookie) {}
    @Override
    public boolean containsHeader(String name) { return headers.containsKey(name); }
    @Override
    public String encodeURL(String url) { return url; }
    @Override
    public String encodeRedirectURL(String url) { return url; }
    @Override
    public void sendError(int sc, String msg) throws IOException {}
    @Override
    public void sendError(int sc) throws IOException {}
    @Override
    public void sendRedirect(String location) throws IOException {}
    @Override
    public void setDateHeader(String name, long date) {}
    @Override
    public void addDateHeader(String name, long date) {}
    @Override
    public void setIntHeader(String name, int value) {}
    @Override
    public void addIntHeader(String name, int value) {}
    @Override
    public void setStatus(int sc, String sm) {}
    @Override
    public void setCharacterEncoding(String charset) {}
    @Override
    public void setContentLengthLong(long length) {}
    @Override
    public void setLocale(Locale loc) {}
    @Override
    public Locale getLocale() { return Locale.getDefault(); }

    @Override
    public String encodeRedirectUrl(String url) {
        return url;  // 简单实现，直接返回原URL
    }

    @Override
    public String encodeUrl(String url) {
        return url;  // 简单实现，直接返回原URL
    }

    @Override
    public void reset() {
        if (isCommitted()) {
            throw new IllegalStateException("Response already committed");
        }
        // 重置响应状态
        status = 200;
        statusMessage = "OK";
        contentType = null;
        contentLength = -1;
        headers.clear();
        
        // 重置writer
        writer = null;
    }

    @Override
    public void resetBuffer() {
        if (writer != null) {
            writer.flush();
        }
    }

    @Override
    public boolean isCommitted() {
        return committed;
    }

    @Override
    public void flushBuffer() throws IOException {
        if (writer != null) {
            writer.flush();
        }
        if (output != null) {
            output.flush();
        }
    }

    @Override
    public void setBufferSize(int size) {
        // 由于我们使用的是基本的 OutputStream，这里不实现缓冲区大小设置
    }

    @Override
    public int getBufferSize() {
        return 0;  // 返回0表示没有使用缓冲
    }
}
