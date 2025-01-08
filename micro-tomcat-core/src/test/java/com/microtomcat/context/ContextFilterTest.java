package com.microtomcat.context;

import com.microtomcat.container.Context;
import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContextFilterTest {
    private Context context;
    private Request mockRequest;
    private Response mockResponse;
    private List<String> filterExecutionOrder;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        context = new Context("", "webroot");
        mockRequest = mock(Request.class);
        mockResponse = mock(Response.class);
        filterExecutionOrder = new ArrayList<>();
    }

    @Test
    void testFilterChainExecution() throws Exception {
        // 创建测试用的 Filter
        Filter firstFilter = createTestFilter("FirstFilter");
        Filter secondFilter = createTestFilter("SecondFilter");
        
        // 创建测试用的 Servlet
        Servlet testServlet = createTestServlet("TestServlet");
        
        // 注册 Filters 和 Servlet
        context.addFilter("first", firstFilter);
        context.addFilter("second", secondFilter);
        context.addServlet("test", testServlet);
        
        // 添加映射
        context.addFilterMapping("/*", "first");
        context.addFilterMapping("/*", "second");
        context.addServletMapping("/test", "test");
        
        // 设置请求 URI
        when(mockRequest.getRequestURI()).thenReturn("/test");
        
        // 执行请求
        context.invoke(mockRequest, mockResponse);
        
        // 验证执行顺序
        List<String> expectedOrder = Arrays.asList(
            "FirstFilter-before",
            "SecondFilter-before",
            "TestServlet",
            "SecondFilter-after",
            "FirstFilter-after"
        );
        
        // 验证列表大小
        assertEquals(expectedOrder.size(), filterExecutionOrder.size(), 
            "Incorrect number of executions");
        
        // 验证每个执行步骤
        for (int i = 0; i < expectedOrder.size(); i++) {
            assertEquals(expectedOrder.get(i), filterExecutionOrder.get(i),
                "Incorrect execution at step " + (i + 1));
        }
    }

    @Test
    void testFilterURLPatternMatching() throws Exception {
        // 创建测试用的 Filter
        Filter pathFilter = createTestFilter("PathFilter");
        Filter extensionFilter = createTestFilter("ExtensionFilter");
        
        // 创建测试用的 Servlet
        Servlet testServlet = createTestServlet("TestServlet");
        
        // 注册 Filters 和 Servlet
        context.addFilter("path", pathFilter);
        context.addFilter("extension", extensionFilter);
        context.addServlet("test", testServlet);
        
        // 添加不同的 URL 模式映射
        context.addFilterMapping("/secure/*", "path");
        context.addFilterMapping("*.jsp", "extension");
        context.addServletMapping("/secure/data", "test");
        
        // 测试路径匹配
        when(mockRequest.getRequestURI()).thenReturn("/secure/data");
        when(mockRequest.getServletPath()).thenReturn("/secure/data");
        when(mockRequest.getPathInfo()).thenReturn(null);
        
        // 清除之前的执行记录
        filterExecutionOrder.clear();
        
        // 执行请求
        context.invoke(mockRequest, mockResponse);
        
        // 打印当前的执行记录，帮助调试
        System.out.println("Filter execution order for /secure/data:");
        filterExecutionOrder.forEach(System.out::println);
        
        assertTrue(filterExecutionOrder.contains("PathFilter-before"),
            "Path filter should be executed");
        
        // 清除执行记录
        filterExecutionOrder.clear();
        
        // 测试扩展名匹配
        when(mockRequest.getRequestURI()).thenReturn("/page.jsp");
        when(mockRequest.getServletPath()).thenReturn("/page.jsp");
        when(mockRequest.getPathInfo()).thenReturn(null);
        context.addServletMapping("*.jsp", "test");
        
        context.invoke(mockRequest, mockResponse);
        
        // 打印当前的执行记录，帮助调试
        System.out.println("Filter execution order for /page.jsp:");
        filterExecutionOrder.forEach(System.out::println);
        
        assertTrue(filterExecutionOrder.contains("ExtensionFilter-before"),
            "Extension filter should be executed");
    }

    @Test
    void testFilterInitAndDestroy() throws Exception {
        // 创建带有初始化和销毁逻辑的 Filter
        Filter filter = new Filter() {
            private boolean initialized = false;
            
            @Override
            public void init(FilterConfig filterConfig) {
                initialized = true;
                filterExecutionOrder.add("Filter-init");
            }
            
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, 
                    FilterChain chain) throws IOException, ServletException {
                if (!initialized) {
                    throw new ServletException("Filter not initialized");
                }
                filterExecutionOrder.add("Filter-doFilter");
                chain.doFilter(request, response);
            }
            
            @Override
            public void destroy() {
                filterExecutionOrder.add("Filter-destroy");
            }
        };
        
        // 添加和映射 Filter
        context.addFilter("lifecycle", filter);
        context.addFilterMapping("/*", "lifecycle");
        
        // 验证初始化
        assertTrue(filterExecutionOrder.contains("Filter-init"),
            "Filter should be initialized");
        
        // 执行请求
        when(mockRequest.getRequestURI()).thenReturn("/test");
        context.invoke(mockRequest, mockResponse);
        
        // 销毁 Context
        context.destroy();
        
        // 验证销毁
        assertTrue(filterExecutionOrder.contains("Filter-destroy"),
            "Filter should be destroyed");
    }

    private Filter createTestFilter(String name) {
        return new Filter() {
            @Override
            public void init(FilterConfig filterConfig) {}

            @Override
            public void doFilter(ServletRequest request, ServletResponse response, 
                    FilterChain chain) throws IOException, ServletException {
                filterExecutionOrder.add(name + "-before");
                chain.doFilter(request, response);
                filterExecutionOrder.add(name + "-after");
            }

            @Override
            public void destroy() {}
        };
    }

    private Servlet createTestServlet(String name) {
        return new Servlet() {
            @Override
            public void init(ServletConfig config) {}

            @Override
            public ServletConfig getServletConfig() {
                return null;
            }

            @Override
            public void service(ServletRequest req, ServletResponse res) {
                filterExecutionOrder.add(name);
            }

            @Override
            public String getServletInfo() {
                return name;
            }

            @Override
            public void destroy() {}
        };
    }
} 