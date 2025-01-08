package com.microtomcat.context;

import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import com.microtomcat.container.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.Servlet;
import java.io.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContextServletMappingTest {
    private Context context;
    
    @Mock
    private Request mockRequest;
    
    @Mock
    private Response mockResponse;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        context = new Context("", "webroot");
    }

    @Test
    void testDispatcherServletPriority() throws Exception {
        // 创建一个 dispatcher servlet
        Servlet dispatcherServlet = mock(Servlet.class);
        context.addServlet("dispatcherServlet", dispatcherServlet);

        // 创建一个普通的 servlet
        Servlet normalServlet = mock(Servlet.class);
        context.addServlet("normalServlet", normalServlet);
        context.addServletMapping("/test", "normalServlet");

        // 设置请求 URI
        when(mockRequest.getRequestURI()).thenReturn("/test");

        // 执行请求
        context.invoke(mockRequest, mockResponse);

        // 验证 dispatcher servlet 被调用，而普通 servlet 没有被调用
        verify(dispatcherServlet, times(1)).service(any(), any());
        verify(normalServlet, never()).service(any(), any());
    }

    @Test
    void testServletMappingPriorityOrder() throws Exception {
        // 创建多个 servlet
        Servlet exactServlet = mock(Servlet.class);
        Servlet prefixServlet = mock(Servlet.class);
        Servlet extensionServlet = mock(Servlet.class);
        Servlet defaultServlet = mock(Servlet.class);

        // 添加 servlet 和映射
        context.addServlet("exactServlet", exactServlet);
        context.addServlet("prefixServlet", prefixServlet);
        context.addServlet("extensionServlet", extensionServlet);
        context.addServlet("defaultServlet", defaultServlet);

        context.addServletMapping("/test.jsp", "exactServlet");
        context.addServletMapping("/test/*", "prefixServlet");
        context.addServletMapping("*.jsp", "extensionServlet");
        context.addServletMapping("/*", "defaultServlet");

        // 测试精确匹配优先
        when(mockRequest.getRequestURI()).thenReturn("/test.jsp");
        context.invoke(mockRequest, mockResponse);
        verify(exactServlet, times(1)).service(any(), any());
        verify(prefixServlet, never()).service(any(), any());
        verify(extensionServlet, never()).service(any(), any());
        verify(defaultServlet, never()).service(any(), any());

        // 测试前缀匹配次之
        reset(exactServlet, prefixServlet, extensionServlet, defaultServlet);
        when(mockRequest.getRequestURI()).thenReturn("/test/something.html");
        context.invoke(mockRequest, mockResponse);
        verify(exactServlet, never()).service(any(), any());
        verify(prefixServlet, times(1)).service(any(), any());
        verify(extensionServlet, never()).service(any(), any());
        verify(defaultServlet, never()).service(any(), any());

        // 测试扩展名匹配再次
        reset(exactServlet, prefixServlet, extensionServlet, defaultServlet);
        when(mockRequest.getRequestURI()).thenReturn("/other.jsp");
        context.invoke(mockRequest, mockResponse);
        verify(exactServlet, never()).service(any(), any());
        verify(prefixServlet, never()).service(any(), any());
        verify(extensionServlet, times(1)).service(any(), any());
        verify(defaultServlet, never()).service(any(), any());

        // 测试默认匹配最后
        reset(exactServlet, prefixServlet, extensionServlet, defaultServlet);
        when(mockRequest.getRequestURI()).thenReturn("/something-else");
        context.invoke(mockRequest, mockResponse);
        verify(exactServlet, never()).service(any(), any());
        verify(prefixServlet, never()).service(any(), any());
        verify(extensionServlet, never()).service(any(), any());
        verify(defaultServlet, times(1)).service(any(), any());
    }

    @Test
    void testRootContextMapping() throws Exception {
        // 创建一个 root servlet
        Servlet rootServlet = mock(Servlet.class);
        context.addServlet("rootServlet", rootServlet);
        context.addServletMapping("/", "rootServlet");

        // 测试根路径请求
        when(mockRequest.getRequestURI()).thenReturn("/");
        context.invoke(mockRequest, mockResponse);
        verify(rootServlet, times(1)).service(any(), any());
    }

    @Test
    void testMultipleRootMappings() throws Exception {
        // 创建两个 servlet
        Servlet firstRootServlet = mock(Servlet.class);
        Servlet secondRootServlet = mock(Servlet.class);
        
        // 添加第一个 root servlet
        context.addServlet("firstRootServlet", firstRootServlet);
        context.addServletMapping("/", "firstRootServlet");
        
        // 添加第二个 root servlet
        context.addServlet("secondRootServlet", secondRootServlet);
        context.addServletMapping("/", "secondRootServlet");
        
        // 测试根路径请求
        when(mockRequest.getRequestURI()).thenReturn("/");
        context.invoke(mockRequest, mockResponse);
        
        // 验证只有最后添加的 servlet 被调用
        verify(firstRootServlet, never()).service(any(), any());
        verify(secondRootServlet, times(1)).service(any(), any());
    }

    @Test
    void testConflictingServletMappings() throws Exception {
        // 创建多个 servlet
        Servlet firstServlet = mock(Servlet.class);
        Servlet secondServlet = mock(Servlet.class);
        
        // 添加相同路径的映射
        context.addServlet("firstServlet", firstServlet);
        context.addServlet("secondServlet", secondServlet);
        
        // 测试不同类型的冲突映射
        
        // 1. 精确路径冲突
        context.addServletMapping("/test", "firstServlet");
        context.addServletMapping("/test", "secondServlet");
        
        when(mockRequest.getRequestURI()).thenReturn("/test");
        context.invoke(mockRequest, mockResponse);
        verify(firstServlet, never()).service(any(), any());
        verify(secondServlet, times(1)).service(any(), any());
        
        // 重置 mock
        reset(firstServlet, secondServlet);
        
        // 2. 通配符路径冲突
        context.addServletMapping("/*", "firstServlet");
        context.addServletMapping("/*", "secondServlet");
        
        when(mockRequest.getRequestURI()).thenReturn("/anything");
        context.invoke(mockRequest, mockResponse);
        verify(firstServlet, never()).service(any(), any());
        verify(secondServlet, times(1)).service(any(), any());
        
        // 重置 mock
        reset(firstServlet, secondServlet);
        
        // 3. 扩展名冲突
        context.addServletMapping("*.jsp", "firstServlet");
        context.addServletMapping("*.jsp", "secondServlet");
        
        when(mockRequest.getRequestURI()).thenReturn("/test.jsp");
        context.invoke(mockRequest, mockResponse);
        verify(firstServlet, never()).service(any(), any());
        verify(secondServlet, times(1)).service(any(), any());
    }

    @Test
    void testComplexPathMatching() throws Exception {
        // 创建多个 servlet
        Servlet exactServlet = mock(Servlet.class);
        Servlet longerPrefixServlet = mock(Servlet.class);
        Servlet shorterPrefixServlet = mock(Servlet.class);
        Servlet extensionServlet = mock(Servlet.class);
        Servlet defaultServlet = mock(Servlet.class);

        // 配置所有 mock servlet
        doNothing().when(exactServlet).init(any());
        doNothing().when(longerPrefixServlet).init(any());
        doNothing().when(shorterPrefixServlet).init(any());
        doNothing().when(extensionServlet).init(any());
        doNothing().when(defaultServlet).init(any());

        // 配置 service 方法不抛出异常
        doNothing().when(exactServlet).service(any(), any());
        doNothing().when(longerPrefixServlet).service(any(), any());
        doNothing().when(shorterPrefixServlet).service(any(), any());
        doNothing().when(extensionServlet).service(any(), any());
        doNothing().when(defaultServlet).service(any(), any());

        // 添加 servlet 和映射
        context.addServlet("exactServlet", exactServlet);
        context.addServlet("longerPrefixServlet", longerPrefixServlet);
        context.addServlet("shorterPrefixServlet", shorterPrefixServlet);
        context.addServlet("extensionServlet", extensionServlet);
        context.addServlet("defaultServlet", defaultServlet);

        // 添加不同的映射
        context.addServletMapping("/foo/bar/test.jsp", "exactServlet");
        context.addServletMapping("/foo/bar/*", "longerPrefixServlet");
        context.addServletMapping("/foo/*", "shorterPrefixServlet");
        context.addServletMapping("*.jsp", "extensionServlet");
        context.addServletMapping("/*", "defaultServlet");

        // 1. 测试精确匹配
        when(mockRequest.getRequestURI()).thenReturn("/foo/bar/test.jsp");
        when(mockRequest.getServletPath()).thenReturn("/foo/bar/test.jsp");
        context.invoke(mockRequest, mockResponse);
        
        verify(exactServlet, times(1)).service(any(), any());
        verify(longerPrefixServlet, never()).service(any(), any());
        verify(shorterPrefixServlet, never()).service(any(), any());
        verify(extensionServlet, never()).service(any(), any());
        verify(defaultServlet, never()).service(any(), any());

        // 2. 测试最长路径前缀匹配
        reset(exactServlet, longerPrefixServlet, shorterPrefixServlet, extensionServlet, defaultServlet);
        when(mockRequest.getRequestURI()).thenReturn("/foo/bar/other.jsp");
        context.invoke(mockRequest, mockResponse);
        verify(longerPrefixServlet, times(1)).service(any(), any());
        verifyNoInteractions(exactServlet, shorterPrefixServlet, extensionServlet, defaultServlet);

        // 3. 测试短路径前缀匹配
        reset(exactServlet, longerPrefixServlet, shorterPrefixServlet, extensionServlet, defaultServlet);
        when(mockRequest.getRequestURI()).thenReturn("/foo/test.jsp");
        context.invoke(mockRequest, mockResponse);
        verify(shorterPrefixServlet, times(1)).service(any(), any());
        verifyNoInteractions(exactServlet, longerPrefixServlet, extensionServlet, defaultServlet);

        // 4. 测试扩展名匹配
        reset(exactServlet, longerPrefixServlet, shorterPrefixServlet, extensionServlet, defaultServlet);
        when(mockRequest.getRequestURI()).thenReturn("/other/test.jsp");
        context.invoke(mockRequest, mockResponse);
        verify(extensionServlet, times(1)).service(any(), any());
        verifyNoInteractions(exactServlet, longerPrefixServlet, shorterPrefixServlet, defaultServlet);

        // 5. 测试默认 servlet
        reset(exactServlet, longerPrefixServlet, shorterPrefixServlet, extensionServlet, defaultServlet);
        when(mockRequest.getRequestURI()).thenReturn("/something-else.html");
        context.invoke(mockRequest, mockResponse);
        verify(defaultServlet, times(1)).service(any(), any());
        verifyNoInteractions(exactServlet, longerPrefixServlet, shorterPrefixServlet, extensionServlet);
    }

    @Test
    void testWelcomeFileList() throws Exception {
        // 创建一个 mock servlet
        Servlet welcomeServlet = mock(Servlet.class);
        doNothing().when(welcomeServlet).init(any());
        doNothing().when(welcomeServlet).service(any(), any());
        
        // 添加 servlet 和映射
        context.addServlet("welcomeServlet", welcomeServlet);
        context.addServletMapping("/", "welcomeServlet");  // 修改为根路径映射

        // 设置请求
        when(mockRequest.getRequestURI()).thenReturn("/");
        when(mockRequest.getServletPath()).thenReturn("/");
        when(mockRequest.getPathInfo()).thenReturn(null);
        when(mockRequest.getMethod()).thenReturn("GET");
        
        // 执行请求
        context.invoke(mockRequest, mockResponse);

        // 验证 servlet 调用
        verify(welcomeServlet, atLeastOnce()).init(any());
        verify(welcomeServlet, times(1)).service(any(), any());
    }
} 