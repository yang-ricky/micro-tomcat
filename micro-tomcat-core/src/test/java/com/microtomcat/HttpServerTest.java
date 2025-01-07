package com.microtomcat;

import com.microtomcat.context.Context;
import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.ServletException;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class HttpServerTest {
    
    private HttpServer server;
    
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
        server = new HttpServer(8080);
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
        String requestUri = "/api/users/current";
        
        // 模拟 Context 的行为
        doAnswer(invocation -> {
            Response response = invocation.getArgument(1);
            response.setContentType("application/json");
            response.setStatus(200);
            response.getWriter().write("{\"username\":\"test\"}");
            response.getWriter().flush();
            return null;
        }).when(mockContext).service(any(Request.class), any(Response.class));

        // 执行请求处理
        server.handleRequest(mockSocket);

        // 验证响应
        String response = outputStream.toString();
        System.out.println("Actual response: " + response);
        assertTrue(response.contains("HTTP/1.1 200"), "Response should contain status 200");
        assertTrue(response.contains("Content-Type: application/json"), "Response should contain correct content type");
        assertTrue(response.contains("{\"username\":\"test\"}"), "Response should contain the expected JSON");
        
        // 验证 Context 是否被正确调用
        verify(mockContext, times(1)).service(any(Request.class), any(Response.class));
    }

    @Test
    void testHandleRequestWithoutContext() throws Exception {
        // 创建一个没有 Context 的服务器
        HttpServer serverWithoutContext = new HttpServer(8080);
        
        // 执行请求处理
        serverWithoutContext.handleRequest(mockSocket);

        // 验证响应是 404
        String response = outputStream.toString();
        assertTrue(response.contains("HTTP/1.1 404"));
        assertTrue(response.contains("Content-Type: text/plain"));
        assertTrue(response.contains("404 Not Found: /api/users/current"));
    }

    @Test
    void testHandleRequestWithContextException() throws Exception {
        // 模拟 Context 抛出异常
        doThrow(new ServletException("Test error"))
            .when(mockContext).service(any(Request.class), any(Response.class));

        // 执行请求处理
        server.handleRequest(mockSocket);

        // 验证响应是 500
        String response = outputStream.toString();
        assertTrue(response.contains("HTTP/1.1 500"));
        assertTrue(response.contains("Content-Type: text/plain"));
        assertTrue(response.contains("500 Internal Server Error"));
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
        HttpServer testServer = new HttpServer(8080);
        
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
}