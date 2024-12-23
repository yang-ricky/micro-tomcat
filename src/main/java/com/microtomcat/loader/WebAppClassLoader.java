package com.microtomcat.loader;

import java.io.*;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.microtomcat.servlet.HttpServlet;
import com.microtomcat.servlet.Servlet;
import com.microtomcat.servlet.ServletException;

public class WebAppClassLoader extends MicroTomcatClassLoader {
    private final String webAppPath;
    private final Map<String, Servlet> servletCache = new ConcurrentHashMap<>();

    public WebAppClassLoader(String webAppPath, ClassLoader parent) throws IOException {
        super(new URL[0], parent);
        this.webAppPath = webAppPath;

        // 1. 添加当前应用的类路径
        addRepository(webAppPath + "/WEB-INF/classes");

        // 2. 如果是根Context，添加其他必要的Web应用特定类路径(如 jar)
        if (webAppPath.equals("webroot")) {
            // addRepository("webroot/WEB-INF/lib");
        }

        log("Class path repositories:");
        for (File path : repositoryPaths) {
            log(" - " + path.getAbsolutePath());
        }
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            log("Attempting to load class: " + name);

            // 1. 先检查是否已加载
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                log("Class already loaded: " + name + " by " + c.getClassLoader());
                return c;
            }

            // 如果是 com.microtomcat.servlet. 下的类，则一定交给父加载器
            if (name.startsWith("com.microtomcat.servlet.")) {
                log("Delegating " + name + " to parent");
                return getParent().loadClass(name);
            }

            // ============ 额外加的关键逻辑: 如果是 HelloServlet，也只给父加载器 ============
            if (name.equals("com.microtomcat.example.HelloServlet")) {
                log("Delegating HelloServlet to parent");
                return getParent().loadClass(name);
            }

            // 其余类按默认双亲委派(先 parent, 再自己)
            return super.loadClass(name);
        }
    }

    // 缓存已加载的Servlet实例
    public Servlet loadServlet(String servletPath) throws ServletException {
        try {
            if (servletCache.containsKey(servletPath)) {
                return servletCache.get(servletPath);
            }

            String servletName = servletPath.substring(servletPath.lastIndexOf("/") + 1);
            Class<?> servletClass;

            // 如果是根Context(""), 则可能要加载 com.microtomcat.example.HelloServlet
            // 但我们已经在 loadClass() 里对 HelloServlet 做了「交给 parent」的逻辑
            // 所以这里依然写: loadClass("com.microtomcat.example."+ servletName)
            // 就能从 parent 拿到
            if (webAppPath.equals("webroot")) {
                servletClass = loadClass("com.microtomcat.example." + servletName);
            } else {
                // 对 /app1、/app2，直接用 "App1Servlet"/"App2Servlet" 这种类名
                servletClass = loadClass(servletName);
            }

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

    public void destroy() {
        servletCache.values().forEach(Servlet::destroy);
        servletCache.clear();
        try {
            close();
        } catch (IOException e) {
            log("Error destroying WebAppClassLoader: " + e.getMessage());
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        log("Finding class: " + name);

        // 将类名转换为文件路径
        String classFilePath = name.replace('.', '/') + ".class";
        File classFile = new File(webAppPath + "/WEB-INF/classes", classFilePath);
        log("Looking for application servlet class: " + classFile.getAbsolutePath());
        byte[] classData = loadClassData(classFile);

        if (classData == null) {
            throw new ClassNotFoundException("Could not find class: " + name);
        }

        return defineClass(name, classData, 0, classData.length);
    }

    private byte[] loadClassData(File classFile) {
        if (!classFile.exists()) {
            return null;
        }

        try (FileInputStream fis = new FileInputStream(classFile);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            log("Found class file: " + classFile.getAbsolutePath());
            return bos.toByteArray();
        } catch (IOException e) {
            log("Error reading class file: " + classFile + ", error: " + e.getMessage());
            return null;
        }
    }
}
