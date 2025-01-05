package com.microtomcat.context;

import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import com.microtomcat.container.Wrapper;
import com.microtomcat.servlet.Servlet;
import com.microtomcat.servlet.ServletException;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.Rule;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class ContextTest {
    private Context context;
    private Request mockRequest;
    private Response mockResponse;
    private PrintWriter mockWriter;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException, ServletException {
        // 创建测试所需的目录结构
        File webappRoot = tempFolder.newFolder("webapps", "test");
        File webInfDir = new File(webappRoot, "WEB-INF");
        File classesDir = new File(webInfDir, "classes");
        classesDir.mkdirs();

        // 使用临时目录创建 Context
        context = new Context("/test", webappRoot.getAbsolutePath());
        mockRequest = mock(Request.class);
        mockResponse = mock(Response.class);
        mockWriter = mock(PrintWriter.class);
        
        // 设置基本的mock行为
        when(mockResponse.getWriter()).thenReturn(mockWriter);
    }

    @Test
    public void testServletHandling() throws Exception {
        // 创建一个测试 Servlet
        Servlet testServlet = new Servlet() {
            @Override
            public void init() throws ServletException {
                // 初始化逻辑
            }

            @Override
            public void service(Request request, Response response) throws ServletException, IOException {
                response.getWriter().write("Test Servlet Response");
            }

            @Override
            public void destroy() {
                // 销毁逻辑
            }
        };

        // 创建 Wrapper 并设置 Servlet
        Wrapper wrapper = new Wrapper("test", "TestServlet");
        wrapper.setServlet(testServlet);  // 直接设置 Servlet 实例
        context.addChild(wrapper);

        // 设置请求 URI 以匹配 Servlet 映射
        when(mockRequest.getUri()).thenReturn("/test/servlet/test");
        
        // 调用上下文的处理方法
        context.invoke(mockRequest, mockResponse);

        // 验证预期行为
        verify(mockResponse, atLeastOnce()).getWriter();
        verify(mockWriter, atLeastOnce()).write("Test Servlet Response");
    }

    @Test
    public void testStaticResourceHandling() throws Exception {
        // 创建一个测试用的静态文件
        File indexFile = new File(tempFolder.getRoot(), "webapps/test/index.html");
        indexFile.getParentFile().mkdirs();
        java.nio.file.Files.write(indexFile.toPath(), "Test Content".getBytes());

        // 设置请求 URI
        when(mockRequest.getUri()).thenReturn("/test/index.html");
        
        // 调用上下文的处理方法
        context.invoke(mockRequest, mockResponse);

        // 验证响应
        verify(mockResponse, never()).sendError(anyInt(), anyString());
    }

    @After
    public void tearDown() {
        tempFolder.delete();
    }
} 