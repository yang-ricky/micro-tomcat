package com.microtomcat.loader;

import java.io.*;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebAppClassLoader extends MicroTomcatClassLoader {
    private final String webAppPath;
    private final Map<String, Long> resourceModifiedTimes = new ConcurrentHashMap<>();
    private final Map<String, Class<?>> loadedClasses = new ConcurrentHashMap<>();
    
    public WebAppClassLoader(String webAppPath, ClassLoader parent) throws IOException {
        super(new URL[0], parent);
        this.webAppPath = webAppPath;
        
        // 添加 WEB-INF/classes 目录
        addRepository(webAppPath + "/WEB-INF/classes");
        // 添加 WEB-INF/lib 目录中的所有 jar
        addJars(new File(webAppPath + "/WEB-INF/lib"));
        // 添加开发环境的类路径
        addRepository("target/classes");
        // 如果在 IDE 中运行，也添加编译输出目录
        addRepository("build/classes");
        
        // 记录当前的类路径
        log("Class path repositories:");
        for (File path : repositoryPaths) {
            log(" - " + path.getAbsolutePath());
        }
    }
    
    private void addJars(File libDir) throws IOException {
        if (libDir.exists() && libDir.isDirectory()) {
            File[] jarFiles = libDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jarFiles != null) {
                for (File jar : jarFiles) {
                    addURL(jar.toURI().toURL());
                }
            }
        }
    }
    
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            // 检查是否已加载
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                return c;
            }

            // 对于 servlet 包下的类，必须委托给父加载器
            if (name.startsWith("com.microtomcat.servlet.")) {
                return getParent().loadClass(name);
            }

            // 对于 example 包下的类，先加载父类，再自己加载
            if (name.startsWith("com.microtomcat.example.")) {
                try {
                    // 先加载父类和接口
                    Class<?> servletInterface = getParent().loadClass("com.microtomcat.servlet.Servlet");
                    Class<?> httpServletClass = getParent().loadClass("com.microtomcat.servlet.HttpServlet");
                    
                    // 然后自己加载 example 包的类
                    byte[] classData = loadClassData(name);
                    if (classData == null) {
                        throw new ClassNotFoundException("Could not load class data for: " + name);
                    }
                    
                    // 定义类之前确保父类已加载
                    resolveClass(httpServletClass);
                    
                    // 定义类
                    c = defineClass(name, classData, 0, classData.length);
                    
                    // 验证类型关系
                    if (!servletInterface.isAssignableFrom(c)) {
                        throw new ClassNotFoundException("Loaded class " + name + " does not implement Servlet interface");
                    }
                    
                    // 解析类
                    resolveClass(c);
                    
                    log("Successfully loaded example class locally: " + name);
                    return c;
                } catch (Exception e) {
                    log("Failed to load example class: " + name + ", error: " + e.getMessage());
                    throw new ClassNotFoundException("Failed to load class: " + name, e);
                }
            }
            
            // 其他类走正常的双亲委派
            try {
                return getParent().loadClass(name);
            } catch (ClassNotFoundException e) {
                return findClass(name);
            }
        }
    }
    
    private byte[] loadClassData(String name) {
        String path = name.replace('.', '/') + ".class";
        
        // 尝试从各个仓库加载
        for (File repo : repositoryPaths) {
            File classFile = new File(repo, path);
            if (classFile.exists()) {
                try (FileInputStream fis = new FileInputStream(classFile);
                     ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                    
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        bos.write(buffer, 0, bytesRead);
                    }
                    return bos.toByteArray();
                } catch (IOException e) {
                    log("Error reading class file: " + classFile);
                }
            }
        }
        return null;
    }
    
    private boolean needsReload(String className) {
        String resourcePath = className.replace('.', '/') + ".class";
        File classFile = new File(webAppPath + "/WEB-INF/classes/" + resourcePath);
        
        if (classFile.exists()) {
            Long lastLoadTime = resourceModifiedTimes.get(className);
            return lastLoadTime != null && classFile.lastModified() > lastLoadTime;
        }
        return false;
    }
    
    private void recordLoadTime(String className) {
        resourceModifiedTimes.put(className, System.currentTimeMillis());
    }
    
    public void destroy() {
        try {
            // 清理资源
            resourceModifiedTimes.clear();
            loadedClasses.clear();
            // 关闭类加载器
            close();
        } catch (IOException e) {
            log("Error destroying WebAppClassLoader: " + e.getMessage());
        }
    }
} 