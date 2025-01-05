package com.microtomcat.servlet;

import org.junit.Before;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class ServletLoaderTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    private ServletLoader loader;
    private File webRoot;
    private File classesPath;
    
    @Before
    public void setUp() throws Exception {
        // 创建临时目录结构
        webRoot = tempFolder.newFolder("webroot");
        File webInf = new File(webRoot, "WEB-INF");
        classesPath = new File(webInf, "classes");
        classesPath.mkdirs();
        
        // 添加项目target/classes到classpath，确保能找到基础类
        String targetClasses = new File("target/classes").getAbsolutePath();
        System.setProperty("java.class.path", 
            System.getProperty("java.class.path") + File.pathSeparator + targetClasses);
        
        // 创建ServletLoader实例
        loader = new ServletLoader(webRoot.getAbsolutePath(), classesPath.getAbsolutePath());
    }
    
    @After
    public void tearDown() {
        if (loader != null) {
            loader.destroy();
        }
    }
    
    @Test
    public void testLoadServletFromWebRoot() throws Exception {
        // 创建测试Servlet类文件
        String servletCode = 
            "import com.microtomcat.servlet.Servlet;\n" +
            "import com.microtomcat.servlet.ServletException;\n" +
            "import com.microtomcat.connector.Request;\n" +
            "import com.microtomcat.connector.Response;\n" +
            "import java.io.IOException;\n" +
            "\n" +
            "public class TestServlet implements Servlet {\n" +
            "    public void init() throws ServletException {}\n" +
            "    public void service(Request request, Response response) " +
            "            throws ServletException, IOException {\n" +
            "        response.getWriter().write(\"Test Response\");\n" +
            "    }\n" +
            "    public void destroy() {}\n" +
            "}";
            
        File servletFile = new File(classesPath, "TestServlet.java");
        Files.write(servletFile.toPath(), servletCode.getBytes());
        
        // 编译Servlet
        compileServlet(servletFile);
        
        // 测试加载
        Servlet servlet = loader.loadServlet("/servlet/TestServlet");
        assertNotNull("Servlet should not be null", servlet);
        assertTrue("Should be instance of Servlet", servlet instanceof Servlet);
    }
    
    @Test
    public void testLoadServletFromClassesPath() throws Exception {
        // 在WEB-INF/classes中创建Servlet
        String servletCode = 
            "import com.microtomcat.servlet.Servlet;\n" +
            "import com.microtomcat.servlet.ServletException;\n" +
            "import com.microtomcat.connector.Request;\n" +
            "import com.microtomcat.connector.Response;\n" +
            "import java.io.IOException;\n" +
            "\n" +
            "public class ClassesServlet implements Servlet {\n" +
            "    public void init() throws ServletException {}\n" +
            "    public void service(Request request, Response response) " +
            "            throws ServletException, IOException {}\n" +
            "    public void destroy() {}\n" +
            "}";
            
        File servletFile = new File(classesPath, "ClassesServlet.java");
        Files.write(servletFile.toPath(), servletCode.getBytes());
        
        compileServlet(servletFile);
        
        Servlet servlet = loader.loadServlet("/servlet/ClassesServlet");
        assertNotNull("Servlet should not be null", servlet);
    }
    
    @Test
    public void testServletCache() throws Exception {
        // 创建测试Servlet
        String servletCode = 
            "import com.microtomcat.servlet.Servlet;\n" +
            "import com.microtomcat.servlet.ServletException;\n" +
            "import com.microtomcat.connector.Request;\n" +
            "import com.microtomcat.connector.Response;\n" +
            "import java.io.IOException;\n" +
            "\n" +
            "public class CachedServlet implements Servlet {\n" +
            "    public void init() throws ServletException {}\n" +
            "    public void service(Request request, Response response) " +
            "            throws ServletException, IOException {}\n" +
            "    public void destroy() {}\n" +
            "}";
            
        File servletFile = new File(classesPath, "CachedServlet.java");
        Files.write(servletFile.toPath(), servletCode.getBytes());
        
        compileServlet(servletFile);
        
        // 加载同一个Servlet两次，应该返回相同实例
        Servlet servlet1 = loader.loadServlet("/servlet/CachedServlet");
        Servlet servlet2 = loader.loadServlet("/servlet/CachedServlet");
        
        assertSame("Should return cached instance", servlet1, servlet2);
    }
    
    private void compileServlet(File sourceFile) throws IOException {
        // 使用JavaCompiler编译Servlet
        javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        int result = compiler.run(null, null, null, 
            "-cp", System.getProperty("java.class.path"),
            "-d", classesPath.getAbsolutePath(),
            sourceFile.getPath());
            
        if (result != 0) {
            throw new IOException("Compilation failed");
        }
    }
} 