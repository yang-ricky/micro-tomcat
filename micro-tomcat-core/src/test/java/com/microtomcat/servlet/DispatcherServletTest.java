package com.microtomcat.servlet;

import com.microtomcat.context.Context;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.File;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DispatcherServletTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    private Context context;
    private HttpServletRequest mockRequest;
    private HttpServletResponse mockResponse;
    private StringWriter stringWriter;
    private MockDispatcherServlet dispatcherServlet;
    
    @Before
    public void setUp() throws Exception {
        // 创建测试目录结构
        File testDir = tempFolder.newFolder("test");
        File webInfDir = new File(testDir, "WEB-INF");
        File classesDir = new File(webInfDir, "classes");
        classesDir.mkdirs();
        
        // 创建测试用的 Context
        context = new Context("testContext", testDir.getAbsolutePath());
        
        // 创建并配置 mock 对象
        mockRequest = mock(HttpServletRequest.class);
        mockResponse = mock(HttpServletResponse.class);
        stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(mockResponse.getWriter()).thenReturn(writer);
        
        // 创建并注册 MockDispatcherServlet
        dispatcherServlet = new MockDispatcherServlet();
        context.addServlet("dispatcherServlet", dispatcherServlet);
        
        // 设置基本的请求属性
        when(mockRequest.getRequestURI()).thenReturn("/test/hello");
        when(mockRequest.getContextPath()).thenReturn("/test");
        when(mockRequest.getMethod()).thenReturn("GET");
    }
    
    @Test
    public void testDispatch() throws ServletException, IOException {
        // 直接使用 mock 对象
        dispatcherServlet.service(mockRequest, mockResponse);
        
        // 验证响应
        verify(mockResponse).setContentType("application/json;charset=UTF-8");
        assertTrue(stringWriter.toString().contains("Hello from TestController"));
    }
    
    @After
    public void tearDown() throws IOException {
        if (stringWriter != null) {
            stringWriter.close();
        }
        tempFolder.delete();
    }
} 