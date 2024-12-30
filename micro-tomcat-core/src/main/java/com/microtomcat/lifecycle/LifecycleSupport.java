package com.microtomcat.lifecycle;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LifecycleSupport {
    private final Lifecycle lifecycle;
    private final List<LifecycleListener> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService asyncEventExecutor = Executors.newSingleThreadExecutor();

    public LifecycleSupport(Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public void addLifecycleListener(LifecycleListener listener) {
        listeners.add(listener);
    }

    public void removeLifecycleListener(LifecycleListener listener) {
        listeners.remove(listener);
    }

    public LifecycleListener[] findLifecycleListeners() {
        return listeners.toArray(new LifecycleListener[0]);
    }

    public void fireLifecycleEvent(String type, Object data) {
        fireLifecycleEvent(type, data, false);
    }

    public void fireLifecycleEvent(String type, Object data, boolean async) {
        LifecycleEvent event = new LifecycleEvent(lifecycle, type, data);
        if (async) {
            asyncEventExecutor.submit(() -> notifyListeners(event));
        } else {
            notifyListeners(event);
        }
    }

    private void notifyListeners(LifecycleEvent event) {
        for (LifecycleListener listener : listeners) {
            try {
                listener.lifecycleEvent(event);
            } catch (Exception e) {
                System.err.println("Error notifying lifecycle listener: " + e.getMessage());
            }
        }
    }

    public void shutdown() {
        asyncEventExecutor.shutdown();
    }
} 