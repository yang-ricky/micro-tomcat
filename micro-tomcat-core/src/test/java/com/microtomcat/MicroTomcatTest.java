package com.microtomcat;

import com.microtomcat.context.Context;
import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class MicroTomcatTest {
    
    private MicroTomcat server;
    
    @Mock
    private Context mockContext;
    
    @Mock
    private Socket mockSocket;
    
    @Mock
    private ExecutorService mockExecutorService;

    private ByteArrayOutputStream outputStream;
    private ByteArrayInputStream inputStream;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        server = new MicroTomcat(8080);
        server.setContext(mockContext);
        outputStream = new ByteArrayOutputStream();
        
        // 设置基本的请求内容
        String request = 
            "GET /api/users/current HTTP/1.1\r\n" +
            "Host: localhost:8080\r\n" +
            "User-Agent: Mozilla/5.0\r\n" +
            "\r\n";
        
        inputStream = new ByteArrayInputStream(request.getBytes());
        
        // 设置 Socket mock
        when(mockSocket.getInputStream()).thenReturn(inputStream);
        when(mockSocket.getOutputStream()).thenReturn(outputStream);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.stop();
        inputStream.close();
        outputStream.close();
    }

    @Test
    void testHandleRequestWithContext() throws Exception {
        // 准备测试数据
        String requestContent = 
            "GET /api/users/current HTTP/1.1\r\n" +
            "Host: localhost:8080\r\n" +
            "\r\n";
        
        inputStream = new ByteArrayInputStream(requestContent.getBytes());
        when(mockSocket.getInputStream()).thenReturn(inputStream);
        
        // 模拟 Context 的行为
        doAnswer(invocation -> {
            Response response = invocation.getArgument(1);
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
            PrintWriter writer = response.getWriter();
            writer.write("{\"username\":\"test\"}");
            writer.flush();
            response.flushBuffer();  // 确保响应被刷新
            return null;
        }).when(mockContext).invoke(any(Request.class), any(Response.class));

        // 执行请求处理
        server.handleRequest(mockSocket);

        // 验证响应
        String response = outputStream.toString();
        System.out.println("Actual response: " + response);
        assertTrue(response.contains("HTTP/1.1 200"), "Response should contain status 200");
        assertTrue(response.contains("Content-Type: application/json"), "Response should contain correct content type");
        assertTrue(response.contains("{\"username\":\"test\"}"), "Response should contain the expected JSON");
    }

    @Test
    void testHandleRequestWithoutContext() throws Exception {
        // 创建一个没有 Context 的服务器
        MicroTomcat serverWithoutContext = new MicroTomcat(8080);
        
        // 准备请求内容
        String requestContent = 
            "GET /api/users/current HTTP/1.1\r\n" +
            "Host: localhost:8080\r\n" +
            "\r\n";
        
        // 创建新的输入输出流
        ByteArrayOutputStream testOutput = new ByteArrayOutputStream();
        ByteArrayInputStream testInput = new ByteArrayInputStream(requestContent.getBytes());
        
        // 创建新的 mock socket
        Socket testSocket = mock(Socket.class);
        when(testSocket.getInputStream()).thenReturn(testInput);
        when(testSocket.getOutputStream()).thenReturn(testOutput);
        
        // 执行请求处理
        serverWithoutContext.handleRequest(testSocket);

        // 验证响应是 404
        String response = testOutput.toString();
        System.out.println("Response without context:\n" + response);
        assertTrue(response.contains("HTTP/1.1 404"), "Response should contain 404 status");
        assertTrue(response.contains("Content-Type: text/plain"), "Response should contain text/plain content type");
        assertTrue(response.contains("404 Not Found: /api/users/current"), "Response should contain error message");
    }

    @Test
    void testHandleRequestWithContextException() throws Exception {
        // 模拟 Context 抛出异常
        doThrow(new RuntimeException("Test error"))
            .when(mockContext).invoke(any(Request.class), any(Response.class));

        // 执行请求处理
        server.handleRequest(mockSocket);

        // 验证响应是 500
        String response = outputStream.toString();
        assertTrue(response.contains("HTTP/1.1 500"), "Response should contain 500 status");
        assertTrue(response.contains("Content-Type: text/plain"), "Response should contain text/plain content type");
        assertTrue(response.contains("500 Internal Server Error"), "Response should contain error message");
    }

    @Test
    void testHandleInvalidRequest() throws Exception {
        // 准备无效的请求数据
        String invalidRequest = "INVALID REQUEST\r\n\r\n";
        InputStream invalidInputStream = new ByteArrayInputStream(invalidRequest.getBytes());
        when(mockSocket.getInputStream()).thenReturn(invalidInputStream);

        // 执行请求处理
        server.handleRequest(mockSocket);

        // 验证没有调用 Context
        verify(mockContext, never()).service(any(Request.class), any(Response.class));
    }

    @Test
    void testServerStartAndStop() throws Exception {
        // 创建一个可以被中断的服务器
        MicroTomcat testServer = new MicroTomcat(8080);
        
        Thread serverThread = new Thread(() -> {
            try {
                testServer.start();
            } catch (Exception e) {
                // 忽略预期的中断异常
            }
        });
        
        serverThread.start();
        Thread.sleep(1000);  // 给服务器一点时间启动
        
        // 先中断线程，再停止服务器
        serverThread.interrupt();
        testServer.stop();
        
        // 给足够的时间让服务器线程结束
        serverThread.join(5000);  // 等待最多5秒
        
        // 验证服务器线程已经结束
        assertFalse(serverThread.isAlive(), "Server thread should not be alive");
        
        // 确保清理资源
        try {
            testServer.stop();
        } catch (Exception e) {
            // 忽略清理过程中的异常
        }
    }

    /**
     * 辅助方法：发送请求并获取响应
     */
    private String sendRequest(String requestContent) throws Exception {
        ByteArrayInputStream testInput = new ByteArrayInputStream(requestContent.getBytes());
        ByteArrayOutputStream testOutput = new ByteArrayOutputStream();
        
        when(mockSocket.getInputStream()).thenReturn(testInput);
        when(mockSocket.getOutputStream()).thenReturn(testOutput);
        
        server.handleRequest(mockSocket);
        
        return testOutput.toString();
    }

    @Test
    void testAddContextAndServlet() throws Exception {
        // 创建新的服务器实例
        MicroTomcat testServer = new MicroTomcat(8080);
        
        // 添加 Context
        Context context = testServer.addContext("", "webroot");
        testServer.setContext(context);  // 确保设置 context
        
        // 创建一个简单的测试 Servlet
        HttpServlet testServlet = new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
                throws ServletException, IOException {
                resp.setContentType("text/plain");
                resp.getWriter().write("test servlet response");
                resp.getWriter().flush();  // 确保内容被写入
            }
        };
        
        // 添加 Servlet 到 Context
        testServer.addServlet(context, "testServlet", testServlet);
        testServer.addServletMapping(context, "/testServlet", "testServlet");
        
        // 验证 Servlet 是否正确添加
        assertNotNull(context.findChild("testServlet"), "Servlet should be registered");
        
        // 发送请求
        String requestContent = 
            "GET /testServlet HTTP/1.1\r\n" +
            "Host: localhost:8080\r\n" +
            "\r\n";
            
        // 创建新的输入输出流
        ByteArrayOutputStream testOutput = new ByteArrayOutputStream();
        ByteArrayInputStream testInput = new ByteArrayInputStream(requestContent.getBytes());
        
        // 创建新的 mock socket
        Socket testSocket = mock(Socket.class);
        when(testSocket.getInputStream()).thenReturn(testInput);
        when(testSocket.getOutputStream()).thenReturn(testOutput);
        
        // 处理请求
        testServer.handleRequest(testSocket);
        
        String response = testOutput.toString();
        System.out.println("Complete response:\n" + response);
        
        // 验证响应
        assertTrue(response.contains("HTTP/1.1 200"), 
            "Response should contain status 200");
        assertTrue(response.contains("Content-Type: text/plain"), 
            "Response should contain correct content type");
        assertTrue(response.contains("test servlet response"), 
            "Response should contain servlet output");
    }

    @Test
    void testAddContextWithNullValues() throws Exception {
        MicroTomcat server = new MicroTomcat(8080);
        
        // 测试 null 值
        assertThrows(IllegalArgumentException.class, () -> {
            server.addContext(null, "webroot");
        }, "Should throw exception for null context path");
        
        assertThrows(IllegalArgumentException.class, () -> {
            server.addContext("", null);
        }, "Should throw exception for null docBase");
    }

    @Test
    void testAddServletWithNullValues() throws Exception {
        MicroTomcat server = new MicroTomcat(8080);
        Context context = server.addContext("", "webroot");
        
        // 测试 null 值
        assertThrows(IllegalArgumentException.class, () -> {
            server.addServlet(null, "testServlet", mock(Servlet.class));
        }, "Should throw exception for null context");
        
        assertThrows(IllegalArgumentException.class, () -> {
            server.addServlet(context, null, mock(Servlet.class));
        }, "Should throw exception for null servlet name");
        
        assertThrows(IllegalArgumentException.class, () -> {
            server.addServlet(context, "testServlet", null);
        }, "Should throw exception for null servlet");
    }
}