package com.microtomcat.loader;

import java.net.URL;
import java.io.IOException;
import java.io.File;

/**
 * CatalinaClassLoader 加载 Tomcat 服务器自身的类（比如org.apache.catalina.XXX），
 * 在这儿我们只是在演示，所以主要依赖 parent = CommonClassLoader
 */
public class CatalinaClassLoader extends MicroTomcatClassLoader {

    public CatalinaClassLoader(ClassLoader parent) throws IOException {
        super(new URL[0], parent);

        // 这里如果你有一些 server-specific 的 class 或 jar，可以放到 lib/server 下
        // addRepository("lib/server");
        // 同样地，如果lib/server不存在，会自动 mkDirs()，可以不再多做处理

        // 这里演示不添加任何路径，让它全部走 parent
        log("CatalinaClassLoader: using parent = " + parent);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            log("CatalinaClassLoader is loading class: " + name);
            // 直接走父类的逻辑即可（里面有双亲委派）
            return super.loadClass(name);
        }
    }
}
