package com.microtomcat.loader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public abstract class MicroTomcatClassLoader extends URLClassLoader {
    protected final List<File> repositoryPaths = new ArrayList<>();
    
    public MicroTomcatClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }
    
    protected void addRepository(String path) throws IOException {
        File repository = new File(path);
        if (!repository.exists()) {
            repository.mkdirs();
        }
        if (!repository.isDirectory()) {
            throw new IOException(path + " is not a directory");
        }
        repositoryPaths.add(repository);
        addURL(repository.toURI().toURL());
    }
    
    protected void log(String message) {
        System.out.println("[" + getClass().getSimpleName() + "] " + message);
    }
    
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            log("Attempting to load class: " + name);
            
            // 首先检查是否已经加载
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                log("Class already loaded: " + name);
                return c;
            }
            
            // 实现双亲委派模型
            try {
                c = getParent().loadClass(name);
                log("Class loaded by parent: " + name);
                return c;
            } catch (ClassNotFoundException e) {
                // 父加载器无法加载，尝试自己加载
                log("Parent couldn't load class: " + name + ", trying locally");
            }
            
            // 尝试从本地仓库加载
            try {
                c = findClass(name);
                log("Successfully loaded class locally: " + name);
                return c;
            } catch (ClassNotFoundException e) {
                log("Failed to load class: " + name);
                throw e;
            }
        }
    }
} 