package com.microtomcat.session.distributed;

import com.microtomcat.session.Session;

public interface SessionStoreAdapter {
    void saveSession(Session session);
    Session loadSession(String sessionId);
    void deleteSession(String sessionId);
} 