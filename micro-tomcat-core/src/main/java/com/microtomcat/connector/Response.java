package com.microtomcat.connector;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Locale;
import java.nio.charset.StandardCharsets;

public class Response implements HttpServletResponse {
    private final OutputStream output;
    private final PrintWriter writer;
    private final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final List<Cookie> cookies = new ArrayList<>();
    private String contentType;
    private String characterEncoding = "UTF-8";
    private int contentLength = -1;
    private int status = HttpServletResponse.SC_OK;
    private boolean committed = false;
    private String errorMessage;
    private Locale locale = Locale.getDefault();
    private int bufferSize = 8192;
    private ServletOutputStream servletOutputStream;
    private ByteArrayOutputStream buffer;

    public Response(OutputStream output) {
        this.output = output;
        this.buffer = new ByteArrayOutputStream(bufferSize);
        this.writer = new PrintWriter(new OutputStreamWriter(buffer, StandardCharsets.UTF_8));
        this.servletOutputStream = new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
            }

            @Override
            public void write(int b) throws IOException {
                buffer.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                buffer.write(b, off, len);
            }
        };
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public void setContentType(String type) {
        this.contentType = type;
        if (!committed && type != null) {
            if (!type.contains("charset=") && getCharacterEncoding() != null) {
                headers.put("Content-Type", type + "; charset=" + getCharacterEncoding());
                System.out.println("Response DEBUG: Setting Content-Type header: " + type + "; charset=" + getCharacterEncoding());
            } else {
                headers.put("Content-Type", type);
                System.out.println("Response DEBUG: Setting Content-Type header: " + type);
            }
        }
    }

    @Override
    public Collection<String> getHeaderNames() {
        return headers.keySet();
    }

    @Override
    public Collection<String> getHeaders(String name) {
        String value = headers.get(name.toLowerCase());
        return value != null ? Collections.singletonList(value) : Collections.emptyList();
    }

    @Override
    public String getHeader(String name) {
        return headers.get(name.toLowerCase());
    }

    @Override
    public void addHeader(String name, String value) {
        if (!committed) {
            headers.put(name.toLowerCase(), value);
        }
    }

    @Override
    public void setHeader(String name, String value) {
        if (!committed) {
            headers.put(name, value);
            System.out.println("Response DEBUG: Setting header: " + name + " = " + value);
        }
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (!committed) {
            sendHeaders();
        }
        return writer;
    }

    @Override
    public void setContentLength(int len) {
        if (!isCommitted()) {
            this.contentLength = len;
            headers.put("Content-Length", String.valueOf(len));
            System.out.println("Response DEBUG: Setting Content-Length: " + len);
        }
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public void setStatus(int status, String message) {
        this.status = status;
        this.errorMessage = message;
    }

    @Override
    public void setStatus(int status) {
        setStatus(status, null);
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        if (isCommitted()) {
            throw new IllegalStateException("Response already committed");
        }
        this.status = sc;
        this.errorMessage = msg;
        
        // Set error page content
        setContentType("text/html");
        PrintWriter out = getWriter();
        out.println("<html><head><title>Error</title></head>");
        out.println("<body><h1>HTTP Error " + sc + "</h1>");
        if (msg != null) {
            out.println("<p>" + msg + "</p>");
        }
        out.println("</body></html>");
        out.flush();
        committed = true;
    }

    public void sendStaticResource(File resource) throws IOException {
        if (resource.exists()) {
            try (FileInputStream fis = new FileInputStream(resource)) {
                // 先设置响应头
                setContentLength((int) resource.length());
                String contentType = getContentTypeFromFileName(resource.getName());
                System.out.println("Response DEBUG: Setting Content-Type for static resource: " + contentType);
                setContentType(contentType);
                
                // 确保发送响应头
                sendHeaders();
                
                // 再发送内容
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                output.flush();
                committed = true;
            }
        } else {
            sendError(HttpServletResponse.SC_NOT_FOUND, "Resource not found: " + resource.getName());
        }
    }

    private String getContentTypeFromFileName(String fileName) {
        String contentType;
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            contentType = "text/html";
        } else if (fileName.endsWith(".txt")) {
            contentType = "text/plain";
        } else if (fileName.endsWith(".css")) {
            contentType = "text/css";
        } else if (fileName.endsWith(".js")) {
            contentType = "application/javascript";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            contentType = "image/png";
        } else if (fileName.endsWith(".gif")) {
            contentType = "image/gif";
        } else {
            contentType = "application/octet-stream";
        }
        System.out.println("DEBUG: getContentTypeFromFileName: " + fileName + " -> " + contentType);
        return contentType;
    }

    @Override
    public boolean isCommitted() {
        return committed;
    }

    @Override
    public void reset() {
        if (committed) {
            throw new IllegalStateException("Cannot reset a committed response");
        }
        headers.clear();
        cookies.clear();
        status = HttpServletResponse.SC_OK;
        contentType = null;
        contentLength = -1;
        errorMessage = null;
    }

    @Override
    public void flushBuffer() throws IOException {
        // 确保响应头已发送
        if (!committed) {
            sendHeaders();
        }
        
        // 刷新缓冲区内容
        if (buffer.size() > 0) {
            buffer.writeTo(output);
            buffer.reset();
        }
        
        // 刷新输出流
        writer.flush();
        output.flush();
        committed = true;
    }

    // Add other required methods from HttpServletResponse interface with default implementations
    @Override
    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public void setCharacterEncoding(String charset) {
        this.characterEncoding = charset;
    }

    @Override
    public void addIntHeader(String name, int value) {
        addHeader(name, String.valueOf(value));
    }

    @Override
    public void setIntHeader(String name, int value) {
        setHeader(name, String.valueOf(value));
    }

    @Override
    public void addDateHeader(String name, long date) {
        addHeader(name, formatDate(date));
    }

    @Override
    public void setDateHeader(String name, long date) {
        setHeader(name, formatDate(date));
    }

    private String formatDate(long date) {
        return new Date(date).toGMTString();
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        if (isCommitted()) {
            throw new IllegalStateException("Response already committed");
        }
        
        // Reset the response
        reset();
        
        // Set status code to 302 (Found/Redirect)
        setStatus(HttpServletResponse.SC_FOUND);
        
        // Set the Location header with the redirect URL
        setHeader("Location", location);
        
        // Send a minimal response body
        setContentType("text/html");
        PrintWriter out = getWriter();
        out.println("<html><head><title>Redirect</title></head>");
        out.println("<body><h1>Redirecting to: " + location + "</h1>");
        out.println("<p>If you are not redirected automatically, please click <a href=\"" 
                   + location + "\">here</a>.</p>");
        out.println("</body></html>");
        
        flushBuffer();
    }

    @Override
    public void sendError(int sc) throws IOException {
        sendError(sc, null);
    }

    /**
     * @deprecated As of version 2.1, use encodeRedirectURL(String url) instead
     */
    @Override
    @Deprecated
    public String encodeRedirectUrl(String url) {
        return encodeRedirectURL(url);
    }

    @Override
    public String encodeRedirectURL(String url) {
        // For now, return the URL unchanged
        // In a full implementation, this would add session ID if URL rewriting is enabled
        return url;
    }

    /**
     * @deprecated As of version 2.1, use encodeURL(String url) instead
     */
    @Override
    @Deprecated
    public String encodeUrl(String url) {
        return encodeURL(url);
    }

    @Override
    public String encodeURL(String url) {
        // For now, return the URL unchanged
        // In a full implementation, this would add session ID if URL rewriting is enabled
        return url;
    }

    @Override
    public boolean containsHeader(String name) {
        return headers.containsKey(name.toLowerCase());
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    public void setLocale(Locale loc) {
        if (!isCommitted() && loc != null) {
            locale = loc;
            String language = locale.getLanguage();
            if (language.length() > 0) {
                String country = locale.getCountry();
                String value = language + (country.length() > 0 ? "-" + country : "");
                setHeader("Content-Language", value);
            }
        }
    }

    @Override
    public void resetBuffer() {
        if (isCommitted()) {
            throw new IllegalStateException("Cannot reset buffer - response is already committed");
        }
        
        // 清空输出流
        try {
            writer.flush();
            // 由于我们使用的是 PrintWriter 包装的 OutputStream，
            // 这里我们只能确保刷新缓冲，但不能真正清除已写入的内容
            // 在实际生产环境中，可能需要使用可重置的缓冲流实现
        } catch (Exception e) {
            // 忽略异常
        }
    }

    @Override
    public int getBufferSize() {
        return bufferSize;
    }

    @Override
    public void setBufferSize(int size) {
        if (isCommitted()) {
            throw new IllegalStateException("Cannot set buffer size - response is already committed");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        this.bufferSize = size;
    }

    @Override
    public void setContentLengthLong(long length) {
        if (!isCommitted()) {
            this.contentLength = (int) length;
            headers.put("Content-Length", String.valueOf(length));
            System.out.println("Response DEBUG: Setting Content-Length (long): " + length);
        }
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (!committed) {
            sendHeaders();
        }
        return servletOutputStream;
    }

    public void sendHeaders() throws IOException {
        if (!isCommitted()) {
            // 1. 写入状态行
            String statusLine = String.format("HTTP/1.1 %d %s\r\n", status, getStatusMessage(status));
            output.write(statusLine.getBytes(StandardCharsets.ISO_8859_1));
            
            // 2. 确保基本响应头存在
            setHeader("Server", "MicroTomcat");
            
            // 设置 Content-Type
            if (contentType != null) {
                if (!contentType.contains("charset=") && getCharacterEncoding() != null) {
                    setHeader("Content-Type", contentType + "; charset=" + getCharacterEncoding());
                } else {
                    setHeader("Content-Type", contentType);
                }
            }
            
            // 3. 写入所有响应头
            for (Map.Entry<String, String> header : headers.entrySet()) {
                String headerLine = String.format("%s: %s\r\n", header.getKey(), header.getValue());
                System.out.println("Response DEBUG: Writing header: " + headerLine.trim());
                output.write(headerLine.getBytes(StandardCharsets.ISO_8859_1));
            }
            
            // 4. 写入空行
            output.write("\r\n".getBytes(StandardCharsets.ISO_8859_1));
            
            // 5. 写入缓冲区内容（如果有的话）
            if (buffer.size() > 0) {
                buffer.writeTo(output);
                buffer.reset();
            }
            
            // 6. 刷新输出流
            output.flush();
            
            committed = true;
        }
    }

    private String getStatusMessage(int status) {
        switch (status) {
            case 200: return "OK";
            case 404: return "Not Found";
            case 500: return "Internal Server Error";
            default: return "Unknown";
        }
    }

    public int getContentLength() {
        return contentLength;
    }

    private void ensureHeadersSent() throws IOException {
        if (!committed) {
            sendHeaders();
        }
    }
}
