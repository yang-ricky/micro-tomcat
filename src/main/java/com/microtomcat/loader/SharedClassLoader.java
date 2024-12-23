package com.microtomcat.loader;

import java.net.URL;
import java.io.IOException;
import java.io.File;

public class SharedClassLoader extends MicroTomcatClassLoader {

    public SharedClassLoader(ClassLoader parent) throws IOException {
        super(new URL[0], parent);

        // 如果你有公共库（jar）可以放这里
        // addRepository("lib/shared");

        log("SharedClassLoader: using parent = " + parent);
    }
}
