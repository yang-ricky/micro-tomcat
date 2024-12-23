package com.microtomcat.loader;

import java.io.IOException;

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

        // 4. 设置当前线程的上下文类加载器 (可选)
        Thread.currentThread().setContextClassLoader(commonLoader);
        log("Set context ClassLoader to: " + commonLoader);

        initialized = true;
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
