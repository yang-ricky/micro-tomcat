package com.microtomcat.lifecycle;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class LifecycleBase implements Lifecycle {
    private final LifecycleSupport lifecycleSupport = new LifecycleSupport(this);
    private final ReentrantReadWriteLock lifecycleLock = new ReentrantReadWriteLock();
    private volatile String state = NEW;

    @Override
    public final void init() throws LifecycleException {
        lifecycleLock.writeLock().lock();
        try {
            if (!state.equals(NEW)) {
                throw new LifecycleException("Component already initialized");
            }
            setStateInternal(INITIALIZING, null);
            initInternal();
            setStateInternal(INITIALIZED, null);
        } finally {
            lifecycleLock.writeLock().unlock();
        }
    }

    @Override
    public final void start() throws LifecycleException {
        lifecycleLock.writeLock().lock();
        try {
            if (state.equals(NEW)) {
                init();
            }
            if (!state.equals(INITIALIZED) && !state.equals(STOPPED)) {
                throw new LifecycleException("Component not initialized or stopped");
            }
            setStateInternal(STARTING, null);
            startInternal();
            setStateInternal(STARTED, null);
        } finally {
            lifecycleLock.writeLock().unlock();
        }
    }

    @Override
    public final void stop() throws LifecycleException {
        lifecycleLock.writeLock().lock();
        try {
            if (!state.equals(STARTED)) {
                throw new LifecycleException("Component not started");
            }
            setStateInternal(STOPPING, null);
            stopInternal();
            setStateInternal(STOPPED, null);
        } finally {
            lifecycleLock.writeLock().unlock();
        }
    }

    @Override
    public final void destroy() throws LifecycleException {
        lifecycleLock.writeLock().lock();
        try {
            if (state.equals(STARTED)) {
                stop();
            }
            setStateInternal(DESTROYING, null);
            destroyInternal();
            setStateInternal(DESTROYED, null);
        } finally {
            lifecycleLock.writeLock().unlock();
        }
    }

    @Override
    public final String getState() {
        lifecycleLock.readLock().lock();
        try {
            return state;
        } finally {
            lifecycleLock.readLock().unlock();
        }
    }

    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycleSupport.addLifecycleListener(listener);
    }

    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycleSupport.removeLifecycleListener(listener);
    }

    @Override
    public LifecycleListener[] findLifecycleListeners() {
        return lifecycleSupport.findLifecycleListeners();
    }

    protected void setStateInternal(String state, Object data) {
        this.state = state;
        lifecycleSupport.fireLifecycleEvent(state, data);
    }

    // Abstract methods that must be implemented by components
    protected abstract void initInternal() throws LifecycleException;
    protected abstract void startInternal() throws LifecycleException;
    protected abstract void stopInternal() throws LifecycleException;
    protected abstract void destroyInternal() throws LifecycleException;
} 