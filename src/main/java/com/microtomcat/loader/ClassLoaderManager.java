package com.microtomcat.loader;

import java.io.IOException;

public class ClassLoaderManager {
    private static CommonClassLoader commonLoader;
    private static CatalinaClassLoader catalinaLoader;
    private static SharedClassLoader sharedLoader;
    
    public static void init() throws IOException {
        // 初始化类加载器层次结构
        commonLoader = new CommonClassLoader();
        catalinaLoader = new CatalinaClassLoader(commonLoader);
        sharedLoader = new SharedClassLoader(commonLoader);
    }
    
    public static WebAppClassLoader createWebAppClassLoader(String webAppPath) throws IOException {
        return new WebAppClassLoader(webAppPath, catalinaLoader);
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
} 