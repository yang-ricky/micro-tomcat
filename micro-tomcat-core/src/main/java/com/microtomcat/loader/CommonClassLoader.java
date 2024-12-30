package com.microtomcat.loader;

import java.io.IOException;
import java.io.File;
import java.net.URL;

/**
 * CommonClassLoader 代表 Tomcat 中的 "common" 类加载器，
 * 主要加载一些容器本身的类、示例类，通常来自 target/classes 或额外 jar。
 */
public class CommonClassLoader extends MicroTomcatClassLoader {

    public CommonClassLoader() throws IOException {
        // parent = 系统类加载器 (AppClassLoader)
        super(new URL[0], ClassLoader.getSystemClassLoader());

        // 添加项目编译输出目录，里面包含 com.microtomcat.servlet.*、com.microtomcat.example.* 等
        addRepository("target/classes");

        log("CommonClassLoader repositories:");
        for (File path : repositoryPaths) {
            log(" - " + path.getAbsolutePath());
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // 将类名转换为文件路径
        String classFilePath = name.replace('.', '/') + ".class";
        
        // 在所有仓库路径中查找类文件
        for (File repository : repositoryPaths) {
            File classFile = new File(repository, classFilePath);
            byte[] classData = loadClassData(classFile);
            if (classData != null) {
                // 找到类文件，定义并返回类
                return defineClass(name, classData, 0, classData.length);
            }
        }
        
        // 找不到类文件，抛出异常
        throw new ClassNotFoundException("Could not find class: " + name);
    }
}
