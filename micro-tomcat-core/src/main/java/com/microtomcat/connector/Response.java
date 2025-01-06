package com.microtomcat.connector;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class Response implements HttpServletResponse {
    private final OutputStream output;
    private final PrintWriter writer;
    private final Map<String, String> headers = new HashMap<>();
    private final List<Cookie> cookies = new ArrayList<>();
    private String contentType;
    private String characterEncoding = "UTF-8";
    private int contentLength = -1;
    private int status = HttpServletResponse.SC_OK;
    private boolean committed = false;
    private String errorMessage;
    private Locale locale = Locale.getDefault();
    private int bufferSize = 8192;  // 默认缓冲区大小为8KB
    private ServletOutputStream servletOutputStream;

    public Response(OutputStream output) {
        this.output = output;
        this.writer = new PrintWriter(new OutputStreamWriter(output));
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public void setContentType(String type) {
        this.contentType = type;
        if (!committed) {
            headers.put("Content-Type", type);
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
            headers.put(name.toLowerCase(), value);
        }
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return writer;
    }

    @Override
    public void setContentLength(int len) {
        this.contentLength = len;
        setHeader("Content-Length", String.valueOf(len));
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
                setContentLength((int) resource.length());
                setContentType(getContentTypeFromFileName(resource.getName()));
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
        if (fileName.endsWith(".html")) return "text/html";
        if (fileName.endsWith(".css")) return "text/css";
        if (fileName.endsWith(".js")) return "application/javascript";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        return "text/plain";
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
        if (isCommitted()) {
            return;
        }
        setHeader("Content-Length", String.valueOf(length));
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (writer != null && writer.checkError()) {
            throw new IllegalStateException("PrintWriter has already been obtained");
        }
        if (servletOutputStream == null) {
            servletOutputStream = new ServletOutputStream() {
                @Override
                public void write(int b) throws IOException {
                    output.write(b);
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setWriteListener(WriteListener writeListener) {
                    throw new UnsupportedOperationException("Write listeners are not supported");
                }
            };
        }
        return servletOutputStream;
    }
}
