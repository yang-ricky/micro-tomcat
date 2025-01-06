package com.microtomcat.servlet;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.microtomcat.connector.Response;

public class DefaultServlet extends HttpServlet {
    private ServletConfig config;
    private String webRoot;

    @Override
    public void init(ServletConfig config) throws javax.servlet.ServletException {
        this.config = config;
        super.init(config);
        
        ServletContext servletContext = config.getServletContext();
        this.webRoot = servletContext.getRealPath("/");
        
        // 如果 getRealPath 返回 null，尝试从属性中获取
        if (this.webRoot == null) {
            Object webRootAttr = servletContext.getAttribute("webRoot");
            if (webRootAttr instanceof String) {
                this.webRoot = (String) webRootAttr;
            }
        }
        
        if (this.webRoot == null) {
            throw new javax.servlet.ServletException("Failed to initialize DefaultServlet: webRoot is null");
        }
        System.out.println("Debug DefaultServlet init with webRoot : " + webRoot);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws IOException, javax.servlet.ServletException {
        if (webRoot == null) {
            throw new javax.servlet.ServletException("DefaultServlet not properly initialized: webRoot is null");
        }
        
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        System.out.println("Debug DefaultServlet doGet uri: " + uri);
        System.out.println("Debug DefaultServlet doGet contextPath: " + contextPath);
        System.out.println("Debug DefaultServlet doGet webRoot: " + webRoot);
        
        // 移除上下文路径，获取相对路径
        if (uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        
        // 构建文件的完整路径
        Path filePath = Paths.get(webRoot, uri);
        
        // 检查文件是否存在
        if (!Files.exists(filePath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, 
                "File Not Found: " + uri);
            return;
        }
        
        // 检查是否是目录
        if (Files.isDirectory(filePath)) {
            // 如果是目录，尝试查找欢迎页面
            for (String welcomeFile : new String[]{"index.html", "index.htm"}) {
                Path welcomePath = filePath.resolve(welcomeFile);
                if (Files.exists(welcomePath) && Files.isRegularFile(welcomePath)) {
                    sendFile(welcomePath, response);
                    return;
                }
            }
            // 如果没有找到欢迎页面，返回目录列表或403
            response.sendError(HttpServletResponse.SC_FORBIDDEN, 
                "Directory listing not allowed");
            return;
        }
        
        // 如果是普通文件，发送文件内容
        if (Files.isRegularFile(filePath)) {
            sendFile(filePath, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, 
                "File Not Found: " + uri);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, javax.servlet.ServletException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, 
            "HTTP method POST is not supported by this URL");
    }

    @Override
    public String getServletInfo() {
        return "DefaultServlet handling static resources";
    }

    // 抽取发送文件的逻辑为独立方法
    private void sendFile(Path filePath, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        
        // 设置 Content-Type (不带 charset)
        String contentType = getServletContext().getMimeType(filePath.toString());
        if (contentType != null) {
            response.setContentType(contentType);
        } else {
            if (filePath.toString().toLowerCase().endsWith(".html")) {
                response.setContentType("text/html");
            } else {
                response.setContentType("application/octet-stream");
            }
        }
        
        response.setHeader("Server", "MicroTomcat");
        
        // 读取文件内容
        byte[] content = Files.readAllBytes(filePath);
        System.out.println("DEBUG: File content read: " + new String(content, "UTF-8"));
        response.setContentLength(content.length);
        
        // 写入响应体
        ServletOutputStream outputStream = response.getOutputStream();
        outputStream.write(content);
        outputStream.flush();
        
        System.out.println("DEBUG: DefaultServlet sendFile - Content written");
    }
} 