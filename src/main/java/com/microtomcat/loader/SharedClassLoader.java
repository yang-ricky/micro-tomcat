package com.microtomcat.loader;

import java.net.URL;
import java.io.IOException;

public class SharedClassLoader extends MicroTomcatClassLoader {
    public SharedClassLoader(ClassLoader parent) throws IOException {
        super(new URL[0], parent);
        
        // 添加共享类库路径(若有的话)
        addRepository("lib/shared");
    }
}
