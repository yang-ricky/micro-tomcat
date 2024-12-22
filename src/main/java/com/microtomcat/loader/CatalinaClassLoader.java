package com.microtomcat.loader;

import java.net.URL;
import java.io.IOException;

public class CatalinaClassLoader extends MicroTomcatClassLoader {
    public CatalinaClassLoader(ClassLoader parent) throws IOException {
        super(new URL[0], parent);
        
        // 只添加服务器核心类路径，不包含 servlet-api
        addRepository("lib/server");
    }
    
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            // servlet 包下的类委托给父加载器（CommonClassLoader）
            if (name.startsWith("com.microtomcat.servlet.")) {
                return getParent().loadClass(name);
            }
            
            return super.loadClass(name);
        }
    }
} 