package com.microtomcat.connector;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.io.File;

public class Response {
    private final OutputStream output;
    private final Request request;
    private static final String WEB_ROOT = "webroot";

    public Response(OutputStream output, Request request) {
        this.output = output;
        this.request = request;
    }

    public void sendServletResponse(String content) throws IOException {
        output.write("HTTP/1.1 200 OK\r\n".getBytes());
        output.write("Content-Type: text/html\r\n".getBytes());
        output.write(("Content-Length: " + content.length() + "\r\n").getBytes());
        output.write("\r\n".getBytes());
        output.write(content.getBytes());
        output.flush();
    }

    public void sendError(int statusCode, String message) throws IOException {
        output.write(("HTTP/1.1 " + statusCode + " " + message + "\r\n").getBytes());
        output.write("Content-Type: text/plain\r\n".getBytes());
        output.write(("Content-Length: " + message.length() + "\r\n").getBytes());
        output.write("\r\n".getBytes());
        output.write(message.getBytes());
        output.flush();
    }

    public void sendStaticResource() throws IOException {
        String uri = request.getUri();
        File file = new File(WEB_ROOT, uri);
        
        if (file.exists()) {
            // Read the file content
            byte[] fileContent = Files.readAllBytes(file.toPath());
            
            // Send headers
            output.write("HTTP/1.1 200 OK\r\n".getBytes());
            output.write(("Content-Type: " + getContentType(uri) + "\r\n").getBytes());
            output.write(("Content-Length: " + fileContent.length + "\r\n").getBytes());
            output.write("\r\n".getBytes());
            
            // Send file content
            output.write(fileContent);
            output.flush();
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
        String cookie = String.format("Set-Cookie: %s=%s; Path=/\r\n", name, value);
        try {
            output.write(cookie.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
