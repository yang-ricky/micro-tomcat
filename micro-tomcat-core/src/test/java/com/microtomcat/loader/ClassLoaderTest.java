package com.microtomcat.loader;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.util.Arrays;

public class ClassLoaderTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    private File webappRoot;
    private File classesDir;
    private ClassLoaderManager manager;
    
    @Before
    public void setUp() throws IOException {
        // 创建测试目录结构
        webappRoot = tempFolder.newFolder("webapps", "testapp");
        File webInfDir = new File(webappRoot, "WEB-INF");
        classesDir = new File(webInfDir, "classes");
        classesDir.mkdirs();
        
        // 初始化类加载器管理器
        ClassLoaderManager.init();
    }
    
    @Test
    public void testClassLoaderHierarchy() throws IOException {
        // 测试类加载器层次结构
        ClassLoader commonLoader = ClassLoaderManager.getCommonLoader();
        ClassLoader catalinaLoader = ClassLoaderManager.getCatalinaLoader();
        ClassLoader sharedLoader = ClassLoaderManager.getSharedLoader();
        
        assertNotNull("CommonLoader should not be null", commonLoader);
        assertNotNull("CatalinaLoader should not be null", catalinaLoader);
        assertNotNull("SharedLoader should not be null", sharedLoader);
        
        assertEquals("CatalinaLoader's parent should be CommonLoader", 
                    commonLoader, catalinaLoader.getParent());
        assertEquals("SharedLoader's parent should be CommonLoader", 
                    commonLoader, sharedLoader.getParent());
    }
    
    @Test
    public void testWebAppClassLoader() throws IOException {
        // 创建并测试 WebAppClassLoader
        WebAppClassLoader loader = ClassLoaderManager.createWebAppClassLoader(webappRoot.getAbsolutePath());
        assertNotNull("WebAppClassLoader should not be null", loader);
        assertEquals("WebAppClassLoader's parent should be CommonLoader",
                    ClassLoaderManager.getCommonLoader(), loader.getParent());
    }
    
    @Test
    public void testLoadCustomClass() throws Exception {
        // 创建一个测试类文件
        String className = "TestClass";
        String sourceCode = 
            "public class TestClass {\n" +
            "    public String getMessage() {\n" +
            "        return \"Hello from TestClass\";\n" +
            "    }\n" +
            "}";
        
        File sourceFile = new File(classesDir, className + ".java");
        Files.write(sourceFile.toPath(), sourceCode.getBytes());
        
        // 编译测试类
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int result = compiler.run(null, null, null, sourceFile.getPath());
        assertEquals("Compilation should succeed", 0, result);
        
        // 使用 WebAppClassLoader 加载类
        WebAppClassLoader loader = ClassLoaderManager.createWebAppClassLoader(webappRoot.getAbsolutePath());
        Class<?> loadedClass = loader.loadClass(className);
        
        assertNotNull("Loaded class should not be null", loadedClass);
        assertEquals("Class loader should be WebAppClassLoader", 
                    loader, loadedClass.getClassLoader());
        
        // 测试类的实例化和方法调用
        Object instance = loadedClass.getDeclaredConstructor().newInstance();
        String message = (String) loadedClass.getMethod("getMessage").invoke(instance);
        assertEquals("Hello from TestClass", message);
    }
    
    @Test
    public void testLoadServletClass() throws Exception {
        // 创建一个测试 Servlet 类
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
            "        response.getWriter().write(\"Test Servlet Response\");\n" +
            "    }\n" +
            "    public void destroy() {}\n" +
            "}";
        
        File servletFile = new File(classesDir, "TestServlet.java");
        Files.write(servletFile.toPath(), servletCode.getBytes());
        
        // 编译 Servlet
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int result = compiler.run(null, null, null, 
            "-cp", System.getProperty("java.class.path"), 
            servletFile.getPath());
        assertEquals("Compilation should succeed", 0, result);
        
        // 使用 WebAppClassLoader 加载 Servlet
        WebAppClassLoader loader = ClassLoaderManager.createWebAppClassLoader(webappRoot.getAbsolutePath());
        Class<?> servletClass = loader.loadClass("TestServlet");
        
        assertNotNull("Servlet class should not be null", servletClass);
        assertTrue("Class should implement Servlet interface",
                  Arrays.asList(servletClass.getInterfaces())
                        .contains(com.microtomcat.servlet.Servlet.class));
    }
    
    @After
    public void tearDown() {
        tempFolder.delete();
    }
} 