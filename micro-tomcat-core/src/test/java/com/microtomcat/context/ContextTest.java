package com.microtomcat.context;

import org.junit.Test;
import javax.servlet.*;
import java.io.IOException;
import java.io.File;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import com.microtomcat.container.Wrapper;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ContextTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testServletRegistration() throws Exception {
        // 使用临时文件夹创建测试目录结构
        File testDir = tempFolder.newFolder("test");
        File webInfDir = new File(testDir, "WEB-INF");
        File classesDir = new File(webInfDir, "classes");
        classesDir.mkdirs();
        
        Context context = new Context("testContext", testDir.getAbsolutePath());
        
        // 创建一个简单的 Servlet 实现
        Servlet mockServlet = mock(Servlet.class);
        
        // 使用不带前导斜杠的路径
        String servletPath = "test-servlet";
        context.addServlet(servletPath, mockServlet);
        
        // 使用 findChild 来获取 Wrapper
        Wrapper wrapper = (Wrapper) context.findChild(servletPath);
        assertNotNull("Servlet wrapper should be registered", wrapper);
        
        // 验证 Wrapper 的名称是否正确
        assertEquals("Wrapper name should match servlet path",
                    servletPath, wrapper.getName());
                    
        // 验证 Context 是否正确设置为 Wrapper 的父容器
        assertSame("Context should be the parent of wrapper",
                  context, wrapper.getParent());
    }
} 