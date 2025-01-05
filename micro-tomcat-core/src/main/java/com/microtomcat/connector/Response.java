package com.microtomcat.connector;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import com.microtomcat.session.Session;
import java.io.PrintWriter;

public class Response {
    private final OutputStream output;
    private final Request request;
    private static final String WEB_ROOT = "webroot";
    private final Map<String, String> headers = new HashMap<>();
    private PrintWriter writer;

    public Response(OutputStream output, Request request) {
        this.output = output;
        this.request = request;
    }

    public void sendServletResponse(String content) throws IOException {
        // 获取当前请求的 Session
        Session session = request.getSession();
        if (session != null) {
            addCookie("JSESSIONID", session.getId());
        }
        
        StringBuilder response = new StringBuilder();
        response.append("HTTP/1.1 200 OK\r\n");
        response.append("Content-Type: text/html\r\n");
        response.append("Content-Length: ").append(content.getBytes().length).append("\r\n");
        
        // 添加所有响应头
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            response.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }
        
        response.append("\r\n");
        response.append(content);
        
        output.write(response.toString().getBytes());
        output.flush();
    }

    public void sendError(int statusCode, String message) throws IOException {
        StringBuilder response = new StringBuilder();
        response.append("HTTP/1.1 ").append(statusCode).append(" ");
        
        switch (statusCode) {
            case 200: response.append("OK"); break;
            case 400: response.append("Bad Request"); break;
            case 404: response.append("Not Found"); break;
            case 500: response.append("Internal Server Error"); break;
            default: response.append("Unknown Status");
        }
        
        response.append("\r\n");
        response.append("Content-Type: text/plain\r\n");
        response.append("Content-Length: ").append(message.length()).append("\r\n");
        response.append("\r\n");
        response.append(message);
        
        output.write(response.toString().getBytes());
        output.flush();
    }

    public void sendStaticResource(File file) throws IOException {
        if (!file.exists() || !file.isFile()) {
            sendError(404, "File Not Found: " + file.getPath());
            return;
        }

        byte[] fileContent = Files.readAllBytes(file.toPath());
        
        StringBuilder response = new StringBuilder();
        response.append("HTTP/1.1 200 OK\r\n");
        response.append("Content-Type: ").append(getContentType(file.getName())).append("\r\n");
        response.append("Content-Length: ").append(fileContent.length).append("\r\n");
        
        // 添加所有响应头
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            response.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }
        
        response.append("\r\n");
        
        output.write(response.toString().getBytes());
        output.write(fileContent);
        output.flush();
    }

    @Deprecated
    public void sendStaticResource() throws IOException {
        String uri = request.getUri();
        File file = new File(WEB_ROOT, uri);
        
        if (file.exists() && file.isFile()) {
            sendStaticResource(file);
        } else {
            // File not found - send 404
            String errorMessage = "404 File Not Found: " + uri;
            sendError(404, errorMessage);
        }
    }

    private String getContentType(String uri) {
        if (uri.endsWith(".html")) {
            return "text/html";
        } else if (uri.endsWith(".txt")) {
            return "text/plain";
        } else if (uri.endsWith(".css")) {
            return "text/css";
        } else if (uri.endsWith(".js")) {
            return "application/javascript";
        }
        return "application/octet-stream";
    }

    public void addCookie(String name, String value) {
        headers.put("Set-Cookie", String.format("%s=%s; Path=/", name, value));
    }

    public void setContentType(String type) {
        headers.put("Content-Type", type);
    }

    public String getContentType() {
        return headers.get("Content-Type");
    }

    public void setContentLength(int length) {
        headers.put("Content-Length", String.valueOf(length));
    }

    public PrintWriter getWriter() throws IOException {
        if (writer == null) {
            writer = new PrintWriter(output, true);
        }
        return writer;
    }
}
