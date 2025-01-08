package com.microtomcat.protocol;

import com.microtomcat.processor.ProcessorPool;

public interface Protocol {
    void init() throws Exception;
    void start() throws Exception;
    void stop() throws Exception;
    void setPort(int port);
    int getPort();
    void setProcessorPool(ProcessorPool processorPool);
    boolean isPortAvailable();
} 