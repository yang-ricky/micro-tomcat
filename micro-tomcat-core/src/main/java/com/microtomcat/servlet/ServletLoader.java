package com.microtomcat.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.InvocationTargetException;
import javax.servlet.Servlet;

public class ServletLoader extends URLClassLoader {
    private final String webRoot;
    private final String classesPath;
    private final Map<String, javax.servlet.Servlet> servletCache = new ConcurrentHashMap<>();

    public ServletLoader(String webRoot, String classesPath) throws IOException {
        super(new URL[0], ServletLoader.class.getClassLoader());
        this.webRoot = webRoot;
        this.classesPath = classesPath;
        
        // 添加类路径
        addURL(new File(classesPath).toURI().toURL());
        addURL(new File(webRoot).toURI().toURL());
        
        log("Added classpath: " + webRoot);
        log("Added classpath: " + classesPath);
    }

    public javax.servlet.Servlet loadServlet(String path) throws ServletException {
        try {
            // 检查缓存
            if (servletCache.containsKey(path)) {
                return servletCache.get(path);
            }

            // 从路径中提取类名
            String className = path;
            if (path.startsWith("/servlet/")) {
                className = path.substring("/servlet/".length());
            }
            className = className.replace('/', '.');

            // 尝试加载类
            log("Trying to load class: " + className);
            Class<?> servletClass = loadClass(className);

            // 验证是否实现了 Servlet 接口
            if (!javax.servlet.Servlet.class.isAssignableFrom(servletClass)) {
                throw new ServletException("Class " + className + " is not a Servlet");
            }

            // 实例化 Servlet
            javax.servlet.Servlet servlet = (javax.servlet.Servlet) servletClass.getDeclaredConstructor().newInstance();
            servletCache.put(path, servlet);
            return servlet;

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException | InvocationTargetException e) {
            throw new ServletException("Error loading servlet: " + path, e);
        }
    }

    public void destroy() {
        try {
            close();
        } catch (IOException e) {
            // ignore
        }
    }

    private void log(String message) {
        System.out.println("[ServletLoader] " + message);
    }
} 