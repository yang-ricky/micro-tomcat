package com.microtomcat.loader;

import java.io.*;
import java.net.URL;
import java.io.IOException;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.microtomcat.servlet.Servlet;
import com.microtomcat.servlet.ServletException;

/**
 * WebAppClassLoader 专门加载某个web应用 /appX 下的类(通常是 WEB-INF/classes、WEB-INF/lib/*.jar)
 * 这里仅演示从 WEB-INF/classes 中加载 .class 文件
 */
public class WebAppClassLoader extends MicroTomcatClassLoader {

    private final String webAppPath;
    private final Map<String, Servlet> servletCache = new ConcurrentHashMap<>();

    public WebAppClassLoader(String webAppPath, ClassLoader parent) throws IOException {
        super(new URL[0], parent);
        this.webAppPath = webAppPath;

        // 1. 添加当前 Web 应用的 WEB-INF/classes
        addRepository(webAppPath + "/WEB-INF/classes");

        // 2. 如果有 WEB-INF/lib/*.jar，也可以在这里循环 addRepository(...)
        // 演示省略

        log("WebAppClassLoader for [" + webAppPath + "] repositories:");
        for (File path : repositoryPaths) {
            log(" - " + path.getAbsolutePath());
        }
    }

    /**
     * 重写 loadClass，以实现对 com.microtomcat.servlet.* 以及 HelloServlet 的特殊委派
     */
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            log("WebAppClassLoader[" + webAppPath + "] - Attempting to load class: " + name);

            // 1. 如果已经加载过，直接返回
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                log("Class already loaded: " + name + " by " + c.getClassLoader());
                return c;
            }

            // 2. 对以下包的类，一律交给父加载器
            if (name.startsWith("com.microtomcat.servlet.") || 
                name.startsWith("com.microtomcat.example.") ||  // 添加这一行
                "com.microtomcat.example.HelloServlet".equals(name)) {
                log("Delegating " + name + " to parent because it's a framework class");
                try {
                    return getParent().loadClass(name);
                } catch (ClassNotFoundException e) {
                    log("Parent failed to load " + name + ", will try locally");
                }
            }

            // 3. 其他类先尝试自己加载
            try {
                return findClass(name);
            } catch (ClassNotFoundException e) {
                // 4. 最后再尝试父加载器
                return getParent().loadClass(name);
            }
        }
    }

    /**
     * loadServlet 是你在 Servlet 容器的某处调用，比如:
     *   WebAppClassLoader loader = createWebAppClassLoader(...);
     *   Servlet servlet = loader.loadServlet("/servlet/App1Servlet");
     */
    public Servlet loadServlet(String servletPath) throws ServletException {
        try {
            if (servletCache.containsKey(servletPath)) {
                return servletCache.get(servletPath);
            }

            // 例如 /servlet/App1Servlet => "App1Servlet"
            String servletName = servletPath.substring(servletPath.lastIndexOf("/") + 1);
            Class<?> servletClass;

            // 如果是 rootContext (webroot 对应 path=""),
            // 那么有可能要加载 WebRootServlet、或者你想把 "HelloServlet" 也放这边？
            // 不过我们已经在 loadClass() 里对 HelloServlet 做了"交给 parent"的逻辑
            // 所以这里，如果你想保持旧的com.microtomcat.example.xxx 逻辑，可以这样：
            if (webAppPath.equals("webroot")) {
                String maybeFullName = "com.microtomcat.example." + servletName;
                log("Trying to load as: " + maybeFullName);
                servletClass = loadClass(maybeFullName);
            } else {
                // 对 /app1、/app2，里面通常就是 "App1Servlet"、"App2Servlet"
                log("Trying to load as local class: " + servletName);
                servletClass = loadClass(servletName);
            }

            // 判断是否实现了 Servlet 接口
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

    /**
     * WebAppClassLoader 覆盖 findClass，
     * 以实现"从 webAppPath/WEB-INF/classes 下的 .class 文件"加载
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        log("WebAppClassLoader[" + webAppPath + "] - Finding class: " + name);

        // 将类名转换为文件路径
        String classFilePath = name.replace('.', '/') + ".class";
        File classFile = new File(webAppPath + "/WEB-INF/classes", classFilePath);

        // 如果文件存在，就读入字节定义类
        byte[] classData = loadClassData(classFile);
        if (classData == null) {
            // 如果 loadClassData 返回 null，说明本 webapp 也没这个类
            throw new ClassNotFoundException("Could not find class: " + name);
        }

        // defineClass() 会把这个类定义到当前 WebAppClassLoader 里
        return defineClass(name, classData, 0, classData.length);
    }

    /**
     * 在 WebAppClassLoader 销毁时，把缓存清空
     */
    public void destroy() {
        servletCache.values().forEach(Servlet::destroy);
        servletCache.clear();
        try {
            close();
        } catch (IOException e) {
            log("Error destroying WebAppClassLoader: " + e.getMessage());
        }
    }
}
