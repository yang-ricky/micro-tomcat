package com.microtomcat.loader;

import java.net.URL;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;

public class CommonClassLoader extends MicroTomcatClassLoader {
    public CommonClassLoader() throws IOException {
        // parent = 系统类加载器
        super(new URL[0], ClassLoader.getSystemClassLoader());
        
        // 添加框架核心类路径，也就是 target/classes
        // 里包含 com.microtomcat.servlet.*、com.microtomcat.example.HelloServlet 等容器/示例类
        addRepository("target/classes");
        
        log("CommonClassLoader repositories:");
        for (File path : repositoryPaths) {
            log(" - " + path.getAbsolutePath());
        }
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            log("Attempting to load class: " + name);
            
            // 1. 检查是否已加载
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                log("Class already loaded: " + name + " by " + c.getClassLoader());
                return c;
            }
            
            // 2. 先让父加载器(系统)试试
            try {
                c = getParent().loadClass(name);
                log("Class loaded by parent: " + name);
                return c;
            } catch (ClassNotFoundException e) {
                log("Parent couldn't load class: " + name + ", trying locally");
            }
            
            // 3. 父加载器也加载不了，就本地加载(从 target/classes)
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
