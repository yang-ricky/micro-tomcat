package com.microtomcat.connector;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Response {
    private OutputStream output;
    private Request request;
    private static final String WEB_ROOT = "webroot";

    public Response(OutputStream output, Request request) {
        this.output = output;
        this.request = request;
    }

    public void sendStaticResource() throws IOException {
        Path filePath = Paths.get(WEB_ROOT, request.getUri());
        if (Files.exists(filePath)) {
            sendSuccessResponse(filePath);
        } else {
            sendNotFoundResponse();
        }
    }

    private void sendSuccessResponse(Path filePath) throws IOException {
        byte[] fileContent = Files.readAllBytes(filePath);
        String contentType = getContentType(filePath.toString());
        
        output.write("HTTP/1.1 200 OK\r\n".getBytes());
        output.write(("Content-Type: " + contentType + "\r\n").getBytes());
        output.write(("Content-Length: " + fileContent.length + "\r\n").getBytes());
        output.write("\r\n".getBytes());
        output.write(fileContent);
        output.flush();
    }

    private void sendNotFoundResponse() throws IOException {
        String errorMessage = "404 File Not Found\r\n";
        System.out.println("404 Not Found: " + request.getUri());
        
        output.write("HTTP/1.1 404 Not Found\r\n".getBytes());
        output.write("Content-Type: text/plain\r\n".getBytes());
        output.write(("Content-Length: " + errorMessage.length() + "\r\n").getBytes());
        output.write("\r\n".getBytes());
        output.write(errorMessage.getBytes());
        output.flush();
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
}
