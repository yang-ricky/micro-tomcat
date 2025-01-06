package com.microtomcat.servlet;

import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import org.junit.Before;
import org.junit.Test;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class HttpServletTest {
    private HttpServletRequest mockRequest;
    private HttpServletResponse mockResponse;
    private StringWriter stringWriter;
    
    @Before
    public void setUp() throws IOException {
        // 直接mock HTTP接口
        mockRequest = mock(HttpServletRequest.class);
        mockResponse = mock(HttpServletResponse.class);
        
        // 设置 writer
        stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(mockResponse.getWriter()).thenReturn(writer);
    }
    
    @Test
    public void testDoGet() throws ServletException, IOException {
        HttpServlet servlet = new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest request, HttpServletResponse response) 
                    throws ServletException, IOException {
                response.getWriter().println("Test GET");
            }
        };
        
        when(mockRequest.getMethod()).thenReturn("GET");
        servlet.service(mockRequest, mockResponse);
        assertTrue(stringWriter.toString().contains("Test GET"));
    }
    
    @Test
    public void testLifecycle() throws ServletException {
        final StringBuilder lifecycle = new StringBuilder();
        
        HttpServlet servlet = new HttpServlet() {
            @Override
            public void init() throws ServletException {
                lifecycle.append("init;");
            }
            
            @Override
            protected void doGet(HttpServletRequest request, HttpServletResponse response) 
                    throws ServletException, IOException {
                lifecycle.append("service;");
            }
            
            @Override
            public void destroy() {
                lifecycle.append("destroy;");
            }
        };
        
        servlet.init();
        try {
            when(mockRequest.getMethod()).thenReturn("GET");
            servlet.service(mockRequest, mockResponse);
        } catch (IOException e) {
            fail("Unexpected IOException");
        }
        servlet.destroy();
        
        assertEquals("init;service;destroy;", lifecycle.toString());
    }
    
    @Test
    public void testErrorHandling() throws Exception {
        HttpServlet servlet = new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest request, HttpServletResponse response) 
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