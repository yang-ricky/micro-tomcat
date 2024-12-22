package com.microtomcat.container.event;

import java.util.EventListener;

public interface ContainerListener extends EventListener {
    void containerEvent(ContainerEvent event);
} 