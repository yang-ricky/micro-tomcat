package com.microtomcat.loader;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Vector;

public class ClassLoaderManager {
    private static CommonClassLoader   commonLoader;
    private static CatalinaClassLoader catalinaLoader;
    private static SharedClassLoader   sharedLoader;
    private static boolean initialized = false;

    public static synchronized void init() throws IOException {
        if (initialized) {
            return;
        }

        log("Initializing ClassLoaderManager");

        // 1. 创建 CommonClassLoader (parent = SystemClassLoader)
        commonLoader = new CommonClassLoader();
        log("Created CommonClassLoader: " + commonLoader);

        // 2. CatalinaClassLoader
        catalinaLoader = new CatalinaClassLoader(commonLoader);
        log("Created CatalinaClassLoader: " + catalinaLoader + " with parent: " + commonLoader);

        // 3. SharedClassLoader
        sharedLoader = new SharedClassLoader(commonLoader);
        log("Created SharedClassLoader: " + sharedLoader + " with parent: " + commonLoader);

        // 4. 设置当前线程的上下文类加载器
        Thread.currentThread().setContextClassLoader(commonLoader);
        log("Set context ClassLoader to: " + commonLoader);

        initialized = true;
        
        // 5. 在初始化完成后分析类加载器情况
        //analyzeClassLoaders();
    }

    // 添加类加载器分析方法
    private static void analyzeClassLoaders() {
        log("=== Class Loader Analysis ===");
        
        // 从多个起点开始分析
        log("=== Analysis from current class ===");
        printClassLoaderInfo(ClassLoaderManager.class.getClassLoader(), 0);
        
        log("\n=== Analysis from Thread Context ClassLoader ===");
        printClassLoaderInfo(Thread.currentThread().getContextClassLoader(), 0);
        
        log("\n=== Analysis from System ClassLoader ===");
        printClassLoaderInfo(ClassLoader.getSystemClassLoader(), 0);
        
        log("=== End of Analysis ===");
    }

    private static void printClassLoaderInfo(ClassLoader cl, int level) {
        if (cl == null) {
            printIndent(level);
            log("Bootstrap ClassLoader");
            return;
        }

        printIndent(level);
        log("[ " + cl + " ]");

        try {
            Field classesField = ClassLoader.class.getDeclaredField("classes");
            classesField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Vector<Class<?>> classes = (Vector<Class<?>>) classesField.get(cl);

            // 过滤并打印感兴趣的类
            for (Class<?> c : classes) {
                String className = c.getName();
                // 只打印我们关心的包下的类
                if (className.startsWith("com.microtomcat") || 
                    className.contains("Servlet") ||
                    className.contains("ClassLoader")) {
                    printIndent(level + 1);
                    log("└── " + className);
                }
            }
        } catch (Exception e) {
            printIndent(level + 1);
            log("(Unable to access loaded classes: " + e.getMessage() + ")");
        }

        printClassLoaderInfo(cl.getParent(), level + 1);
    }

    private static void printIndent(int level) {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < level; i++) {
            indent.append("   ");
        }
        log(indent.toString());
    }

    /**
     * 为每个 WebApp 目录创建一个 WebAppClassLoader
     * parent = commonLoader (或者你也可传 sharedLoader、catalinaLoader ...)
     */
    public static WebAppClassLoader createWebAppClassLoader(String webAppPath) throws IOException {
        if (!initialized) {
            init();
        }

        log("Creating WebAppClassLoader for: " + webAppPath);
        log("Using parent loader: " + commonLoader);
        WebAppClassLoader loader = new WebAppClassLoader(webAppPath, commonLoader);
        log("Created WebAppClassLoader: " + loader);
        return loader;
    }

    public static ClassLoader getCommonLoader() {
        return commonLoader;
    }
    public static ClassLoader getCatalinaLoader() {
        return catalinaLoader;
    }
    public static ClassLoader getSharedLoader() {
        return sharedLoader;
    }

    private static void log(String message) {
        System.out.println("[ClassLoaderManager] " + message);
    }
}
