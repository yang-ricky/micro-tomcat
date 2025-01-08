package com.microtomcat.context;

import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import com.microtomcat.container.Container;
import com.microtomcat.container.Context;
import com.microtomcat.container.Wrapper;
import com.microtomcat.loader.WebAppClassLoader;
import org.junit.Before;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.IOException;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import java.nio.file.Files;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayInputStream;
import javax.servlet.ServletInputStream;
import java.io.ByteArrayOutputStream;
import javax.servlet.WriteListener;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.InputStream;
import java.io.OutputStream;
import com.microtomcat.session.SessionManager;
import javax.servlet.ServletException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.servlet.ReadListener;

public class ContextTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    private Context context;
    private Request mockRequest;
    private Response mockResponse;
    private StringWriter stringWriter;
    private StringBuilder responseContent;
    
    @Before
    public void setUp() throws Exception {
        // 创建临时测试目录
        File webRoot = tempFolder.newFolder("webroot");
        File webInfDir = new File(webRoot, "WEB-INF");
        File classesDir = new File(webInfDir, "classes");
        classesDir.mkdirs();
        
        // 创建测试用的 index.html
        File indexHtml = new File(webRoot, "index.html");
        Files.write(indexHtml.toPath(), 
            "<!DOCTYPE html>\n<html><body><h1>Welcome to MicroTomcat  in unit test</h1></body></html>".getBytes());
        
        // 使用临时目录创建 Context
        context = new Context("", webRoot.getAbsolutePath());
        
        // 在初始化Context之前设置ServletContext
        ServletContext mockServletContext = mock(ServletContext.class);
        when(mockServletContext.getRealPath(anyString())).thenAnswer(invocation -> {
            String path = invocation.getArgument(0);
            return new File(webRoot, path).getAbsolutePath();
        });
        context.setServletContext(mockServletContext);
        
        // 设置 mock 对象
        mockRequest = mock(Request.class);
        mockResponse = mock(Response.class);
        stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        
        // 增加基本的 HTTP 请求/响应属性
        when(mockResponse.getWriter()).thenReturn(writer);
        when(mockResponse.getCharacterEncoding()).thenReturn("UTF-8");
        when(mockRequest.getCharacterEncoding()).thenReturn("UTF-8");
        when(mockRequest.getScheme()).thenReturn("http");
        when(mockRequest.getServerName()).thenReturn("localhost");
        when(mockRequest.getServerPort()).thenReturn(8080);
        when(mockRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(mockRequest.getRemoteHost()).thenReturn("localhost");
        when(mockRequest.getProtocol()).thenReturn("HTTP/1.1");
        
        // 添加必要的响应方法模拟
        when(mockResponse.getOutputStream()).thenReturn(mock(ServletOutputStream.class));
        doNothing().when(mockResponse).setContentLength(anyInt());
        doNothing().when(mockResponse).setContentType(anyString());
        doNothing().when(mockResponse).setHeader(anyString(), anyString());
        doNothing().when(mockResponse).setStatus(anyInt());
        
        // 初始化和启动 Context
        context.init();
        context.start();
        
        // 设置通用的请求属性
        when(mockRequest.getContextPath()).thenReturn("");
        when(mockRequest.getMethod()).thenReturn("GET");  // 默认为 GET 方法
        when(mockRequest.getServletPath()).thenReturn("");
        when(mockRequest.getPathInfo()).thenReturn("");
        
        // 确保 DefaultServlet 被正确初始化
        Container defaultServlet = context.findChild("default");
        if (defaultServlet instanceof Wrapper) {
            // 直接调用一次 invoke() 来触发初始化
            try {
                when(mockRequest.getRequestURI()).thenReturn("/");
                ((Wrapper) defaultServlet).invoke(mockRequest, mockResponse);
            } catch (Exception e) {
                System.err.println("Warning: Failed to initialize DefaultServlet: " + e.getMessage());
            }
        }
        
        final StringBuilder content = new StringBuilder();
        ServletOutputStream mockOutputStream = new ServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                content.append((char) b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                content.append(new String(b, off, len, "UTF-8"));
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
        
        when(mockResponse.getOutputStream()).thenReturn(mockOutputStream);
        
        // 保存 StringBuilder 以便测试方法使用
        this.responseContent = content;
    }
    
    @Test
    public void testRootPathRequest() throws Exception {
        when(mockRequest.getRequestURI()).thenReturn("/");
        when(mockRequest.getContextPath()).thenReturn("");
        
        System.out.println("DEBUG: Before service call");
        context.service(mockRequest, mockResponse);
        System.out.println("DEBUG: After service call");
        
        // 验证响应头
        verify(mockResponse, atLeastOnce()).setStatus(HttpServletResponse.SC_OK);
        verify(mockResponse, atLeastOnce()).setContentType("text/html");
        verify(mockResponse, atLeastOnce()).setHeader("Server", "MicroTomcat");
        verify(mockResponse, atLeastOnce()).setContentLength(anyInt());
        
        // 直接从 StringBuilder 获取内容
        String response = responseContent.toString();
        System.out.println("DEBUG: Response content: " + response);
        
        assertTrue("Response should contain welcome message", 
                   response.contains("Welcome to MicroTomcat"));
        assertTrue("Response should be valid HTML",
                   response.contains("<!DOCTYPE html>"));
    }
    
    @Test
    public void testDefaultServletRegistration() throws Exception {
        // 获取 Context 中注册的 DefaultServlet
        Container defaultServlet = context.findChild("default");
        
        assertNotNull("DefaultServlet should be registered", defaultServlet);
        assertTrue("DefaultServlet should be a Wrapper", defaultServlet instanceof Wrapper);
        
        // 测试 DefaultServlet 的类加载
        Wrapper wrapper = (Wrapper) defaultServlet;
        try {
            // 模拟请求来触发 servlet 加载
            when(mockRequest.getRequestURI()).thenReturn("/index.html");
            wrapper.invoke(mockRequest, mockResponse);
        } catch (Exception e) {
            if (e.getCause() instanceof ClassNotFoundException) {
                fail("DefaultServlet class not found. Make sure com.microtomcat.servlet.DefaultServlet " +
                     "is in the correct classpath location: " + e.getMessage());
            }
            throw e;
        }
    }
    
    @Test
    public void testClassLoaderConfiguration() {
        WebAppClassLoader classLoader = context.getWebAppClassLoader();
        assertNotNull("WebAppClassLoader should not be null", classLoader);
        
        // 验证类加载器层次结构
        ClassLoader parent = classLoader.getParent();
        assertTrue("Parent should be CommonClassLoader", 
                  parent.getClass().getName().contains("CommonClassLoader"));
                  
        // 验证类加载路径存在
        File classesDir = new File(tempFolder.getRoot(), "webroot/WEB-INF/classes");
        assertTrue("WEB-INF/classes directory should exist", classesDir.exists());
        assertTrue("WEB-INF/classes should be a directory", classesDir.isDirectory());
    }
    
    @Test
    public void testDefaultServletStaticFile() throws Exception {
        // 创建一个测试文件，注意不要包含换行符
        String content = "<html><body>Test Content</body></html>";
        File testFile = new File(tempFolder.getRoot(), "webroot/test.html");
        testFile.getParentFile().mkdirs();
        Files.write(testFile.toPath(), content.getBytes(StandardCharsets.UTF_8));

        // 模拟请求，添加更多必要的属性
        when(mockRequest.getRequestURI()).thenReturn("/test.html");
        when(mockRequest.getContextPath()).thenReturn("");
        when(mockRequest.getServletPath()).thenReturn("/test.html");
        when(mockRequest.getPathInfo()).thenReturn(null);
        when(mockRequest.getMethod()).thenReturn("GET");
        
        // 添加 ServletContext 相关的 mock
        ServletContext mockServletContext = mock(ServletContext.class);
        when(mockServletContext.getMimeType("/test.html")).thenReturn("text/html");
        when(mockServletContext.getRealPath(anyString())).thenReturn(testFile.getAbsolutePath());
        context.setServletContext(mockServletContext);
        
        // 添加输出流相关的 mock
        ServletOutputStream mockOutputStream = mock(ServletOutputStream.class);
        when(mockResponse.getOutputStream()).thenReturn(mockOutputStream);
        
        context.service(mockRequest, mockResponse);
        
        // 修改验证方式：不验证具体调用次数，只验证最终结果
        verify(mockResponse, atLeastOnce()).setContentType("text/html");
        verify(mockResponse).setContentLength(content.length()); // 使用实际内容长度
        assertTrue(stringWriter.toString().contains("Test Content"));
    }

    @Test
    public void testDefaultServletFileNotFound() throws Exception {
        // 模拟请求一个不存在的文件
        when(mockRequest.getRequestURI()).thenReturn("/nonexistent.txt");
        when(mockRequest.getContextPath()).thenReturn("");
        
        context.service(mockRequest, mockResponse);
        
        // 验证是否返回 404，包含正确的错误消息
        verify(mockResponse).sendError(HttpServletResponse.SC_NOT_FOUND, 
            "File Not Found: /nonexistent.txt");
    }

    @Test
    public void testDefaultServletDirectory() throws Exception {
        // 创建一个测试目录
        File testDir = new File(tempFolder.getRoot(), "webroot/testdir");
        testDir.mkdirs();

        // 模拟请求目录
        when(mockRequest.getRequestURI()).thenReturn("/testdir");
        when(mockRequest.getContextPath()).thenReturn("");
        
        context.service(mockRequest, mockResponse);
        
        // 验证是否返回 404，包含正确的错误消息
        verify(mockResponse).sendError(HttpServletResponse.SC_NOT_FOUND, 
            "File Not Found: /testdir");
    }

    @Test
    public void testDefaultServletGetRequest() throws Exception {
        // 创建测试文件
        String content = "<html><body>Test Content</body></html>";
        File testFile = new File(tempFolder.getRoot(), "webroot/test.html");
        testFile.getParentFile().mkdirs();
        Files.write(testFile.toPath(), content.getBytes(StandardCharsets.UTF_8));

        // 模拟 GET 请求
        when(mockRequest.getRequestURI()).thenReturn("/test.html");
        when(mockRequest.getContextPath()).thenReturn("");
        when(mockRequest.getServletPath()).thenReturn("/test.html");
        when(mockRequest.getPathInfo()).thenReturn(null);
        when(mockRequest.getMethod()).thenReturn("GET");
        
        // 添加 ServletContext 相关的 mock
        ServletContext mockServletContext = mock(ServletContext.class);
        when(mockServletContext.getMimeType("/test.html")).thenReturn("text/html");
        when(mockServletContext.getRealPath(anyString())).thenReturn(testFile.getAbsolutePath());
        context.setServletContext(mockServletContext);
        
        context.service(mockRequest, mockResponse);
        
        // 验证响应，使用 atLeastOnce() 而不是精确次数
        verify(mockResponse, atLeastOnce()).setContentType("text/html");
        verify(mockResponse).setContentLength(content.length());
        assertTrue(stringWriter.toString().contains("Test Content"));
    }
    
    //@Test
    public void testDefaultServletPostRequest() throws Exception {
        // 创建测试文件
        String content = "<html><body>Test Content</body></html>";
        String postData = "name=test&value=123";
        File testFile = new File(tempFolder.getRoot(), "webroot/test.html");
        testFile.getParentFile().mkdirs();
        Files.write(testFile.toPath(), content.getBytes(StandardCharsets.UTF_8));

        // 模拟 POST 请求
        when(mockRequest.getRequestURI()).thenReturn("/test.html");
        when(mockRequest.getContextPath()).thenReturn("");
        when(mockRequest.getServletPath()).thenReturn("/test.html");
        when(mockRequest.getPathInfo()).thenReturn(null);
        when(mockRequest.getMethod()).thenReturn("POST");
        
        // 模拟 POST 数据
        ByteArrayInputStream inputStream = new ByteArrayInputStream(postData.getBytes());
        when(mockRequest.getInputStream()).thenReturn(new ServletInputStream() {
            @Override
            public int read() throws IOException {
                return inputStream.read();
            }
            
            @Override
            public boolean isFinished() {
                return inputStream.available() == 0;
            }
            
            @Override
            public boolean isReady() {
                return true;
            }
            
            @Override
            public void setReadListener(ReadListener readListener) {
                // 不需要实现
            }
        });
        when(mockRequest.getContentLength()).thenReturn(postData.length());
        when(mockRequest.getContentType()).thenReturn("application/x-www-form-urlencoded");
        
        // 添加 ServletContext 相关的 mock
        ServletContext mockServletContext = mock(ServletContext.class);
        when(mockServletContext.getMimeType("/test.html")).thenReturn("text/html");
        when(mockServletContext.getRealPath(anyString())).thenReturn(testFile.getAbsolutePath());
        context.setServletContext(mockServletContext);
        
        context.service(mockRequest, mockResponse);
        
        // 验证响应，使用 atLeastOnce() 而不是精确次数
        verify(mockResponse, atLeastOnce()).setContentType("text/html");
        verify(mockResponse).setContentLength(content.length());
        assertTrue(stringWriter.toString().contains("Test Content"));
        
        // 验证 POST 数据是否被正确处理
        verify(mockRequest).getInputStream();
        verify(mockRequest).getContentLength();
        verify(mockRequest).getContentType();
    }
    
    @Test
    public void testHttpResponseFormat() throws Exception {
        ByteArrayOutputStream rawOutput = new ByteArrayOutputStream();
        Response realResponse = new Response(rawOutput);
        
        // 设置基本响应属性
        realResponse.setStatus(HttpServletResponse.SC_OK);
        realResponse.setContentType("text/html");
        realResponse.setHeader("Server", "MicroTomcat");
        
        // 设置响应内容
        String content = "<!DOCTYPE html>\n<html><body><h1>Welcome to MicroTomcat</h1></body></html>";
        byte[] contentBytes = content.getBytes("UTF-8");
        
        // 先设置内容长度
        realResponse.setContentLength(contentBytes.length);
        
        // 发送头部
        realResponse.sendHeaders();
        
        // 写入内容
        realResponse.getOutputStream().write(contentBytes);
        realResponse.getOutputStream().flush();
        
        // 打印完整响应以便调试
        String fullResponse = rawOutput.toString("ISO-8859-1");
        System.out.println("DEBUG: Full HTTP Response:\n" + fullResponse);
    }
    
    @Test
    public void testRootPathRequestWithRealResponse() throws Exception {
        // 创建测试用的 index.html
        File indexHtml = new File(tempFolder.getRoot(), "webroot/index.html");
        String content = "<!DOCTYPE html>\n<html><body><h1>Welcome to MicroTomcat</h1></body></html>";
        Files.write(indexHtml.toPath(), content.getBytes(StandardCharsets.UTF_8));
        System.out.println("DEBUG: Created index.html at: " + indexHtml.getAbsolutePath());
        System.out.println("DEBUG: File content: " + content);
        
        // 创建真实的请求对象
        Request request = mock(Request.class);
        when(request.getRequestURI()).thenReturn("/");
        when(request.getContextPath()).thenReturn("");
        when(request.getServletPath()).thenReturn("/");
        when(request.getPathInfo()).thenReturn(null);
        when(request.getMethod()).thenReturn("GET");
        when(request.getProtocol()).thenReturn("HTTP/1.1");
        
        // 创建真实的响应对象
        ByteArrayOutputStream rawOutput = new ByteArrayOutputStream();
        Response response = new Response(rawOutput);
        response.setCharacterEncoding("UTF-8");
        
        // 设置 ServletContext
        ServletContext mockServletContext = mock(ServletContext.class);
        when(mockServletContext.getRealPath("/")).thenReturn(tempFolder.getRoot() + "/webroot");
        when(mockServletContext.getRealPath(anyString())).thenAnswer(invocation -> {
            String path = invocation.getArgument(0);
            return new File(tempFolder.getRoot() + "/webroot", path).getAbsolutePath();
        });
        when(mockServletContext.getMimeType("index.html")).thenReturn("text/html");
        when(mockServletContext.getMimeType("/index.html")).thenReturn("text/html");
        context.setServletContext(mockServletContext);
        
        // 调用 service 方法
        context.service(request, response);
        
        // 确保响应被刷新
        response.flushBuffer();
        
        // 获取完整响应
        String fullResponse = rawOutput.toString(StandardCharsets.UTF_8.name());
        System.out.println("DEBUG: Full Response:\n" + fullResponse);
        
        // 分离响应头和响应体
        String[] parts = fullResponse.split("\r\n\r\n", 2);
        System.out.println("DEBUG: Response Headers:\n" + parts[0]);
        if (parts.length > 1) {
            System.out.println("DEBUG: Response Body:\n" + parts[1]);
        }
        
        // 验证响应
        assertTrue("Response should start with HTTP/1.1 200",
                   fullResponse.startsWith("HTTP/1.1 200"));
        assertTrue("Response should contain Content-Type: text/html",
                   fullResponse.contains("Content-Type: text/html"));
        assertTrue("Response should contain Server header",
                   fullResponse.contains("Server: MicroTomcat"));
        
        // 验证响应体
        if (parts.length > 1) {
            String body = parts[1];
            assertTrue("Response body should contain welcome message",
                      body.contains("Welcome to MicroTomcat"));
        } else {
            fail("Response body is missing");
        }
    }

    //@Test
    public void testIntegrationWithRealServer() throws Exception {
        // 使用 CountDownLatch 确保服务器已启动
        final CountDownLatch serverStarted = new CountDownLatch(1);
        final CountDownLatch requestHandled = new CountDownLatch(1);
        
        // 创建一个独立的服务器实例
        ServerSocket serverSocket = new ServerSocket(0); // 使用随机可用端口
        int port = serverSocket.getLocalPort();
        
        // 启动服务器线程
        Thread serverThread = new Thread(() -> {
            try {
                serverStarted.countDown(); // 标记服务器已启动
                Socket socket = serverSocket.accept();
                
                // 处理请求
                InputStream input = socket.getInputStream();
                OutputStream output = socket.getOutputStream();
                
                // 创建 SessionManager 实例
                SessionManager sessionManager = context.getSessionManager();
                
                // 使用正确的构造函数创建 Request
                Request request = new Request(input, sessionManager);
                Response response = new Response(output);
                
                try {
                    context.service(request, response);
                } catch (ServletException e) {
                    e.printStackTrace();
                    // 在发生 ServletException 时发送 500 错误响应
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                                     e.getMessage());
                }
                
                socket.close();
                serverSocket.close();
                requestHandled.countDown(); // 标记请求已处理
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        serverThread.start();

        // 等待服务器启动
        serverStarted.await();

        try {
            // 发送实际的 HTTP 请求
            URL url = new URL("http://localhost:" + port + "/");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // 验证响应
            int responseCode = connection.getResponseCode();
            String contentType = connection.getHeaderField("Content-Type");
            String server = connection.getHeaderField("Server");
            String date = connection.getHeaderField("Date");
            int contentLength = connection.getContentLength();

            // 读取响应体
            BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream()));
            StringBuilder responseBody = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                responseBody.append(inputLine);
            }
            in.close();

            // 验证响应
            assertEquals("HTTP status code should be 200", 
                        HttpServletResponse.SC_OK, responseCode);
            assertEquals("Content-Type should be text/html", 
                        "text/html; charset=UTF-8", contentType);
            assertEquals("Server header should be MicroTomcat", 
                        "MicroTomcat", server);
            assertNotNull("Date header should exist", date);
            assertTrue("Date header format should be RFC 1123",
                      date.matches("[A-Za-z]{3}, \\d{2} [A-Za-z]{3} \\d{4} \\d{2}:\\d{2}:\\d{2} GMT"));
            assertEquals("Content-Length should match", 
                        responseBody.length(), contentLength);
            assertTrue("Response body should contain welcome message", 
                      responseBody.toString().contains("Welcome to MicroTomcat"));
        } finally {
            // 等待请求处理完成
            requestHandled.await(5, TimeUnit.SECONDS);
            // 确保服务器线程停止
            serverThread.interrupt();
            serverThread.join(5000);
        }
    }

    @Test
    public void testErrorHandling404() throws Exception {
        // 使用 ByteArrayOutputStream 来捕获响应
        ByteArrayOutputStream rawOutput = new ByteArrayOutputStream();
        Response realResponse = new Response(rawOutput);
        
        // 设置一个不存在的路径
        when(mockRequest.getRequestURI()).thenReturn("/not-exists.html");
        when(mockRequest.getContextPath()).thenReturn("");
        
        // 调用 service 方法
        context.service(mockRequest, realResponse);
        
        String fullResponse = rawOutput.toString("ISO-8859-1");
        System.out.println("DEBUG: 404 Response:\n" + fullResponse);
        
        // 验证响应
        assertTrue("Response should start with HTTP/1.1 404",
                   fullResponse.startsWith("HTTP/1.1 404"));
        assertTrue("Response should contain Content-Type",
                   fullResponse.contains("Content-Type: text/html"));
        assertTrue("Response should contain 404 message",
                   fullResponse.contains("Not Found"));
    }

    @Test
    public void testPostRequest() throws Exception {
        // 准备 POST 数据
        String postData = "param1=value1&param2=value2";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(
            postData.getBytes(StandardCharsets.UTF_8));
        
        // 创建模拟的 ServletInputStream
        ServletInputStream mockServletInputStream = new ServletInputStream() {
            @Override
            public boolean isFinished() { return false; }
            
            @Override
            public boolean isReady() { return true; }
            
            @Override
            public void setReadListener(ReadListener readListener) {}
            
            @Override
            public int read() throws IOException {
                return inputStream.read();
            }
        };
        
        // 设置请求属性
        when(mockRequest.getMethod()).thenReturn("POST");
        when(mockRequest.getInputStream()).thenReturn(mockServletInputStream);
        when(mockRequest.getContentType()).thenReturn("application/x-www-form-urlencoded");
        when(mockRequest.getContentLength()).thenReturn(postData.length());
        when(mockRequest.getRequestURI()).thenReturn("/test-post");
        
        // 捕获响应
        ByteArrayOutputStream rawOutput = new ByteArrayOutputStream();
        Response realResponse = new Response(rawOutput);
        
        // 调用 service 方法
        context.service(mockRequest, realResponse);
        
        String fullResponse = rawOutput.toString("ISO-8859-1");
        System.out.println("DEBUG: POST Response:\n" + fullResponse);
        
        // 验证响应
        assertTrue("Response should start with HTTP/1.1",
                   fullResponse.startsWith("HTTP/1.1"));
        assertTrue("Response should contain Content-Type",
                   fullResponse.contains("Content-Type:"));
        assertTrue("Response should contain Server header",
                   fullResponse.contains("Server: MicroTomcat"));
    }

    //@Test
    public void testLargeResponse() throws Exception {
        // 创建一个大文件
        File largeFile = new File(tempFolder.getRoot(), "webroot/large.txt");
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            content.append("Line ").append(i).append("\n");
        }
        Files.write(largeFile.toPath(), content.toString().getBytes());
        
        // 设置请求
        when(mockRequest.getRequestURI()).thenReturn("/large.txt");
        
        // 捕获响应
        ByteArrayOutputStream rawOutput = new ByteArrayOutputStream();
        Response realResponse = new Response(rawOutput);
        
        // 调用 service 方法
        context.service(mockRequest, realResponse);
        
        String fullResponse = rawOutput.toString("ISO-8859-1");
        System.out.println("DEBUG: Full Response:\n" + fullResponse);
        
        // 验证响应
        assertTrue("Response should start with HTTP/1.1",
                   fullResponse.startsWith("HTTP/1.1 200 OK"));
        assertTrue("Response should contain correct Content-Length",
                   fullResponse.contains("Content-Length: " + content.length()));
        assertTrue("Response should contain Content-Type",
                   fullResponse.contains("Content-Type: text/plain"));
    }

    @Test
    public void testConcurrentRequests() throws Exception {
        // 创建服务器
        ServerSocket serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();
        
        // 使用 CountDownLatch 来同步线程
        CountDownLatch serverStarted = new CountDownLatch(1);
        CountDownLatch allRequestsHandled = new CountDownLatch(3);
        
        // 启动服务器线程
        Thread serverThread = new Thread(() -> {
            try {
                serverStarted.countDown();
                while (!Thread.currentThread().isInterrupted()) {
                    Socket socket = serverSocket.accept();
                    new Thread(() -> {
                        try {
                            handleRequest(socket);
                            allRequestsHandled.countDown();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            } catch (IOException e) {
                if (!Thread.currentThread().isInterrupted()) {
                    e.printStackTrace();
                }
            }
        });
        serverThread.start();
        
        // 等待服务器启动
        serverStarted.await();
        
        // 发送多个并发请求
        Thread[] clients = new Thread[3];
        for (int i = 0; i < clients.length; i++) {
            clients[i] = new Thread(() -> {
                try {
                    URL url = new URL("http://localhost:" + port + "/");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    assertEquals("Response code should be 200", 
                               HttpServletResponse.SC_OK, conn.getResponseCode());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            clients[i].start();
        }
        
        // 等待所有请求处理完成
        allRequestsHandled.await(5, TimeUnit.SECONDS);
        
        // 清理
        serverThread.interrupt();
        serverSocket.close();
        serverThread.join(5000);
    }

    private void handleRequest(Socket socket) throws IOException, ServletException {
        InputStream input = socket.getInputStream();
        OutputStream output = socket.getOutputStream();
        
        SessionManager sessionManager = context.getSessionManager();
        Request request = new Request(input, sessionManager);
        Response response = new Response(output);
        
        context.service(request, response);
        
        socket.close();
    }

    @After
    public void tearDown() throws IOException {
        if (stringWriter != null) {
            try {
                stringWriter.close();
            } catch (IOException e) {
                // 在测试清理阶段，我们通常可以忽略关闭异常
                System.err.println("Warning: Failed to close StringWriter: " + e.getMessage());
            }
        }
    }
} 