package com.microtomcat.loader;

import java.io.IOException;

public class ClassLoaderManager {
    private static CommonClassLoader commonLoader;
    private static CatalinaClassLoader catalinaLoader;
    private static SharedClassLoader sharedLoader;
    private static boolean initialized = false;
    
    public static synchronized void init() throws IOException {
        if (initialized) {
            return;
        }
        
        log("Initializing ClassLoaderManager");
        
        // 1. 初始化 CommonClassLoader
        commonLoader = new CommonClassLoader();
        log("Created CommonClassLoader: " + commonLoader);
        
        // 2. 初始化其他加载器
        catalinaLoader = new CatalinaClassLoader(commonLoader);
        log("Created CatalinaClassLoader: " + catalinaLoader + " with parent: " + commonLoader);
        
        sharedLoader = new SharedClassLoader(commonLoader);
        log("Created SharedClassLoader: " + sharedLoader + " with parent: " + commonLoader);
        
        // 3. 设置当前线程的上下文类加载器
        Thread.currentThread().setContextClassLoader(commonLoader);
        log("Set context ClassLoader to: " + commonLoader);
        
        initialized = true;
    }
    
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
