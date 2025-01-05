package com.microtomcat.servlet;

import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import org.junit.Before;
import org.junit.Test;
import java.io.IOException;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class HttpServletTest {
    
    private Request mockRequest;
    private Response mockResponse;
    
    @Before
    public void setUp() {
        mockRequest = mock(Request.class);
        mockResponse = mock(Response.class);
    }
    
    @Test
    public void testDefaultGetMethod() throws Exception {
        // 创建一个测试用的HttpServlet实现类
        HttpServlet servlet = new HttpServlet() {
            // 使用默认实现
        };
        
        // 配置GET请求
        when(mockRequest.getMethod()).thenReturn("GET");
        
        // 执行service方法
        servlet.service(mockRequest, mockResponse);
        
        // 验证sendError被调用，因为默认的doGet会返回501
        verify(mockResponse).sendError(eq(501), contains("GET method not implemented"));
    }
    
    @Test
    public void testDefaultPostMethod() throws Exception {
        HttpServlet servlet = new HttpServlet() {
            // 使用默认实现
        };
        
        // 配置POST请求
        when(mockRequest.getMethod()).thenReturn("POST");
        
        servlet.service(mockRequest, mockResponse);
        
        // 验证sendError被调用，因为默认的doPost会返回501
        verify(mockResponse).sendError(eq(501), contains("POST method not implemented"));
    }
    
    @Test
    public void testUnsupportedMethod() throws Exception {
        HttpServlet servlet = new HttpServlet() {
            // 使用默认实现
        };
        
        // 配置一个不支持的HTTP方法
        when(mockRequest.getMethod()).thenReturn("PUT");
        
        servlet.service(mockRequest, mockResponse);
        
        // 验证sendError被调用，返回501
        verify(mockResponse).sendError(eq(501), contains("not implemented"));
    }
    
    @Test
    public void testCustomGetImplementation() throws Exception {
        // 创建一个自定义的GET实现
        HttpServlet servlet = new HttpServlet() {
            @Override
            protected void doGet(Request request, Response response) 
                    throws ServletException, IOException {
                response.sendServletResponse("Custom GET Response");
            }
        };
        
        // 配置GET请求
        when(mockRequest.getMethod()).thenReturn("GET");
        
        servlet.service(mockRequest, mockResponse);
        
        // 验证自定义响应被发送
        verify(mockResponse).sendServletResponse("Custom GET Response");
    }
    
    @Test
    public void testLifecycle() throws Exception {
        // 创建一个跟踪生命周期事件的Servlet
        final StringBuilder lifecycle = new StringBuilder();
        
        HttpServlet servlet = new HttpServlet() {
            @Override
            public void init() throws ServletException {
                lifecycle.append("init;");
            }
            
            @Override
            protected void doGet(Request request, Response response) 
                    throws ServletException, IOException {
                lifecycle.append("service;");
            }
            
            @Override
            public void destroy() {
                lifecycle.append("destroy;");
            }
        };
        
        // 执行完整的生命周期
        when(mockRequest.getMethod()).thenReturn("GET");
        
        servlet.init();
        servlet.service(mockRequest, mockResponse);
        servlet.destroy();
        
        // 验证生命周期顺序
        assertEquals("init;service;destroy;", lifecycle.toString());
    }
    
    @Test
    public void testErrorHandling() throws Exception {
        // 创建一个会抛出异常的Servlet
        HttpServlet servlet = new HttpServlet() {
            @Override
            protected void doGet(Request request, Response response) 
                    throws ServletException, IOException {
                throw new ServletException("Test Exception");
            }
        };
        
        when(mockRequest.getMethod()).thenReturn("GET");
        
        try {
            servlet.service(mockRequest, mockResponse);
            fail("Expected ServletException");
        } catch (ServletException e) {
            assertEquals("Test Exception", e.getMessage());
        }
    }
} 