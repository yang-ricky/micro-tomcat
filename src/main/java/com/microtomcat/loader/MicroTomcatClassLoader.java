package com.microtomcat.loader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 这是所有自定义 ClassLoader 的抽象基类，
 * 提供了通用的添加仓库路径、以及简单的双亲委派逻辑。
 */
public abstract class MicroTomcatClassLoader extends URLClassLoader {
    
    // 存放我们手动添加的仓库（文件夹、jar包等）的绝对路径
    protected final List<File> repositoryPaths = new ArrayList<>();

    public MicroTomcatClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    /**
     * 手动往 ClassLoader 中添加目录或 jar
     */
    protected void addRepository(String path) throws IOException {
        File repository = new File(path);
        if (!repository.exists()) {
            // 如果不存在，就先创建（比如 mkdirs）
            repository.mkdirs();
        }
        if (!repository.isDirectory()) {
            // 也可以进一步判断 jar 文件，这里仅做演示
            throw new IOException(path + " is not a directory");
        }
        repositoryPaths.add(repository);
        // 同时加到 URL 搜索路径中
        addURL(repository.toURI().toURL());
    }

    /**
     * 日志打印，方便调试
     */
    protected void log(String message) {
        System.out.println("[" + getClass().getSimpleName() + "] " + message);
    }

    /**
     * 标准的双亲委派流程
     */
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            log("Attempting to load class: " + name);

            // 1. 如果已经加载过，直接返回
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                log("Class already loaded: " + name + " by " + c.getClassLoader());
                return c;
            }

            // 2. 先走父加载器 (双亲委派)
            try {
                c = getParent().loadClass(name);
                log("Class loaded by parent: " + name);
                return c;
            } catch (ClassNotFoundException e) {
                log("Parent couldn't load class: " + name + ", trying locally");
            }

            // 3. 父加载器也加载不了，就自己 findClass
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

    /**
     * 缺省的 findClass 不做任何事，具体子类会去覆盖它（如果需要自行读取 .class）
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        throw new ClassNotFoundException("findClass not implemented in " + getClass().getSimpleName());
    }

    /**
     * 子类里可以用这个方法根据 File 读字节后 defineClass
     */
    protected byte[] loadClassData(File classFile) {
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
