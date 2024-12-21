package com.microtomcat.servlet;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServletLoader {
    private final String webRoot;
    private final URLClassLoader classLoader;
    private final Map<String, Servlet> servletCache = new ConcurrentHashMap<>();
    private static final String DEFAULT_PACKAGE = "com.microtomcat.example.";
    
    public ServletLoader(String webRoot, String classesDirPath) throws IOException {
        this.webRoot = webRoot;
        File webRootDir = new File(webRoot);
        File classesDir = new File(classesDirPath);
        
        URL[] urls = new URL[]{
            webRootDir.toURI().toURL(),
            classesDir.toURI().toURL()
        };
        
        this.classLoader = new URLClassLoader(urls, this.getClass().getClassLoader());
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
                return classLoader.loadClass(className);
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
            classLoader.close();
        } catch (IOException e) {
            // Log error
        }
    }
} 