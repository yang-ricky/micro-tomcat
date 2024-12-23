package com.microtomcat.loader;

import java.net.URL;
import java.io.IOException;

public class CatalinaClassLoader extends MicroTomcatClassLoader {
    public CatalinaClassLoader(ClassLoader parent) throws IOException {
        super(new URL[0], parent);
        
        // 只添加服务器核心类路径(不含 Servlet API jar 时，也可在这里放 .class)
        // 你可以把 com.microtomcat.servlet.*、HelloServlet 都编译到 target/classes
        // 或者放到 lib/server 里
        addRepository("lib/server");
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            // 不做额外处理，直接走双亲委派
            return super.loadClass(name);
        }
    }
}
