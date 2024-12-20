package com.microtomcat.server;

import java.io.IOException;

public class HttpServerFactory {
    public static AbstractHttpServer createServer(ServerConfig config) throws IOException {
        return config.isNonBlocking() 
            ? new NonBlockingHttpServer(config)
            : new BlockingHttpServer(config);
    }
} 