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

    // 注意，这里并不覆盖 findClass()，父类里默认会抛 ClassNotFoundException
    // 因为我们已经 addURL(...) 进去了 target/classes，所以 URLClassLoader 能直接找到 class
    // 如果想手动做 FileInputStream -> defineClass，也可以覆盖 findClass()
}
