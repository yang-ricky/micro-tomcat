package com.microtomcat.lifecycle;

public interface Lifecycle {
    // Lifecycle event types
    public static final String BEFORE_INIT_EVENT = "before_init";
    public static final String AFTER_INIT_EVENT = "after_init";
    public static final String BEFORE_START_EVENT = "before_start";
    public static final String AFTER_START_EVENT = "after_start";
    public static final String BEFORE_STOP_EVENT = "before_stop";
    public static final String AFTER_STOP_EVENT = "after_stop";
    public static final String BEFORE_DESTROY_EVENT = "before_destroy";
    public static final String AFTER_DESTROY_EVENT = "after_destroy";

    // Lifecycle state constants
    public static final String NEW = "NEW";
    public static final String INITIALIZING = "INITIALIZING";
    public static final String INITIALIZED = "INITIALIZED";
    public static final String STARTING = "STARTING";
    public static final String STARTED = "STARTED";
    public static final String STOPPING = "STOPPING";
    public static final String STOPPED = "STOPPED";
    public static final String DESTROYING = "DESTROYING";
    public static final String DESTROYED = "DESTROYED";
    public static final String FAILED = "FAILED";

    // Lifecycle methods
    void init() throws LifecycleException;
    void start() throws LifecycleException;
    void stop() throws LifecycleException;
    void destroy() throws LifecycleException;

    // Lifecycle state management
    String getState();
    
    // Listener management
    void addLifecycleListener(LifecycleListener listener);
    void removeLifecycleListener(LifecycleListener listener);
    LifecycleListener[] findLifecycleListeners();
} 