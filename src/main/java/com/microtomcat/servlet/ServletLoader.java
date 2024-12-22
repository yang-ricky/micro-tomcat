package com.microtomcat.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServletLoader extends URLClassLoader {
    private final String webRoot;
    private final String classesPath;
    private final Map<String, Servlet> servletCache = new ConcurrentHashMap<>();
    private static final String DEFAULT_PACKAGE = "com.microtomcat.example.";
    
    public ServletLoader(String webRoot, String classesPath) throws IOException {
        super(new URL[0], ServletLoader.class.getClassLoader());
        this.webRoot = webRoot;
        this.classesPath = classesPath;
        
        // 添加类路径
        addClassPath(new File(classesPath));
        // 添加 webroot 目录本身，因为有些类直接放在这里
        addClassPath(new File(webRoot));
        // 添加 src/main/java 路径（用于加载 HelloServlet）
        addClassPath(new File("src/main/java"));
    }

    private void addClassPath(File classPath) throws IOException {
        if (classPath.exists()) {
            addURL(classPath.toURI().toURL());
            log("Added classpath: " + classPath.getAbsolutePath());
        } else {
            log("Classpath not found: " + classPath.getAbsolutePath());
        }
    }

    private void log(String message) {
        System.out.println("[ServletLoader] " + message);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        try {
            log("Trying to load class: " + name);
            return super.loadClass(name);
        } catch (ClassNotFoundException e) {
            // 尝试从不同的位置加载类
            String[] paths = {
                // 1. 直接从 webroot 加载
                webRoot + "/" + name + ".class",
                // 2. 从应用的 WEB-INF/classes 加载
                webRoot + "/app1/WEB-INF/classes/" + name + ".class",
                webRoot + "/app2/WEB-INF/classes/" + name + ".class",
                // 3. 从当前上下文的 WEB-INF/classes 加载
                classesPath + "/" + name + ".class"
            };

            for (String path : paths) {
                File file = new File(path);
                log("Looking for class file: " + file.getAbsolutePath());
                
                if (file.exists()) {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            bos.write(buffer, 0, bytesRead);
                        }
                        byte[] bytes = bos.toByteArray();
                        log("Found and loaded class file: " + file.getAbsolutePath());
                        return defineClass(null, bytes, 0, bytes.length);
                    } catch (IOException ex) {
                        log("Error loading class file: " + ex.getMessage());
                    }
                }
            }
            
            log("Class file not found in any location: " + name);
            throw e;
        }
    }

    public Servlet loadServlet(String servletPath) throws ServletException {
        try {
            if (servletCache.containsKey(servletPath)) {
                return servletCache.get(servletPath);
            }

            String servletName = servletPath.substring(servletPath.lastIndexOf("/") + 1);
            
            Class<?> servletClass = tryLoadServletClass(DEFAULT_PACKAGE + servletName, servletName);
            
            if (!Servlet.class.isAssignableFrom(servletClass)) {
                throw new ServletException("Class " + servletClass.getName() + " is not a Servlet");
            }

            Servlet servlet = (Servlet) servletClass.getDeclaredConstructor().newInstance();
            servlet.init();
            servletCache.put(servletPath, servlet);
            
            return servlet;
        } catch (Exception e) {
            throw new ServletException("Error loading servlet: " + servletPath, e);
        }
    }

    private Class<?> tryLoadServletClass(String... classNames) throws ClassNotFoundException {
        ClassNotFoundException lastException = null;
        
        for (String className : classNames) {
            try {
                return loadClass(className);
            } catch (ClassNotFoundException e) {
                lastException = e;
            }
        }
        
        throw lastException;
    }

    public void destroy() {
        servletCache.values().forEach(Servlet::destroy);
        servletCache.clear();
        try {
            close();
        } catch (IOException e) {
            // Log error
        }
    }
} 