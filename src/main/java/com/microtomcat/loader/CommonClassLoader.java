package com.microtomcat.loader;

import java.net.URL;
import java.io.IOException;
import java.io.File;

public class CommonClassLoader extends MicroTomcatClassLoader {
    public CommonClassLoader() throws IOException {
        super(new URL[0], ClassLoader.getSystemClassLoader());
        
        // 确保 servlet-api 目录存在并包含所需类
        addRepository("lib/servlet-api");
        
        // 添加日志以便调试
        log("CommonClassLoader repositories:");
        for (File path : repositoryPaths) {
            log(" - " + path.getAbsolutePath());
        }
    }
    
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        log("CommonClassLoader attempting to load: " + name);
        
        // servlet 包下的类由 CommonClassLoader 负责加载
        if (name.startsWith("com.microtomcat.servlet.")) {
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                return c;
            }
            try {
                c = findClass(name);
                log("Successfully loaded servlet class: " + name);
                return c;
            } catch (ClassNotFoundException e) {
                log("Failed to load servlet class: " + name);
                throw e;
            }
        }
        
        return super.loadClass(name);
    }
} 