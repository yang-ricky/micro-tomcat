package com.microtomcat.lifecycle;

public class LifecycleEvent {
    private final Lifecycle lifecycle;
    private final String type;
    private final Object data;

    public LifecycleEvent(Lifecycle lifecycle, String type, Object data) {
        this.lifecycle = lifecycle;
        this.type = type;
        this.data = data;
    }

    public Lifecycle getLifecycle() {
        return lifecycle;
    }

    public String getType() {
        return type;
    }

    public Object getData() {
        return data;
    }
} 