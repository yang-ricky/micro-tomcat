package com.microtomcat.servlet;

import org.junit.Before;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import javax.servlet.Servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
        
        // 添加项目target/classes到classpath
        String targetClasses = new File("target/classes").getAbsolutePath();
        System.setProperty("java.class.path", 
            System.getProperty("java.class.path") + File.pathSeparator + targetClasses);
        
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
        copyAndCompileServlet("TestServlet.java");
        
        javax.servlet.Servlet servlet = loader.loadServlet("/servlet/TestServlet");
        assertNotNull("Servlet should not be null", servlet);
        assertTrue("Should be instance of Servlet", servlet instanceof javax.servlet.Servlet);
    }
    
    @Test
    public void testLoadServletFromClassesPath() throws Exception {
        copyAndCompileServlet("ClassesServlet.java");
        
        javax.servlet.Servlet servlet = loader.loadServlet("/servlet/ClassesServlet");
        assertNotNull("Servlet should not be null", servlet);
        assertTrue("Should be instance of Servlet", servlet instanceof javax.servlet.Servlet);
    }
    
    @Test
    public void testServletCache() throws Exception {
        copyAndCompileServlet("CachedServlet.java");
        
        javax.servlet.Servlet servlet1 = loader.loadServlet("/servlet/CachedServlet");
        javax.servlet.Servlet servlet2 = loader.loadServlet("/servlet/CachedServlet");
        
        assertSame("Should return cached instance", servlet1, servlet2);
    }
    
    private void copyAndCompileServlet(String servletFileName) throws IOException {
        // 从资源文件复制Servlet源码
        try (InputStream in = getClass().getResourceAsStream("/servlets/" + servletFileName)) {
            File servletFile = new File(classesPath, servletFileName);
            Files.copy(in, servletFile.toPath());
            compileServlet(servletFile);
        }
    }
    
    private void compileServlet(File sourceFile) throws IOException {
        javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        
        String classpath = System.getProperty("java.class.path") + 
                          File.pathSeparator + 
                          new File("target/classes").getAbsolutePath() +
                          File.pathSeparator + 
                          new File("target/test-classes").getAbsolutePath();
        
        int result = compiler.run(null, null, null, 
            "-cp", classpath,
            "-d", classesPath.getAbsolutePath(),
            sourceFile.getPath());
            
        if (result != 0) {
            throw new IOException("Compilation failed");
        }
    }
} 