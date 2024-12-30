package com.microtomcat.container.event;

import java.util.EventObject;
import com.microtomcat.container.Container;

public class ContainerEvent extends EventObject {
    private final String type;
    private final Object data;

    public ContainerEvent(Container source, String type, Object data) {
        super(source);
        this.type = type;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public Object getData() {
        return data;
    }

    @Override
    public Container getSource() {
        return (Container) super.getSource();
    }

    // 定义标准事件类型
    public static final String CHILD_ADDED = "childAdded";
    public static final String CHILD_REMOVED = "childRemoved";
    public static final String START_EVENT = "start";
    public static final String STOP_EVENT = "stop";
    public static final String INIT_EVENT = "init";
    public static final String DESTROY_EVENT = "destroy";
} 