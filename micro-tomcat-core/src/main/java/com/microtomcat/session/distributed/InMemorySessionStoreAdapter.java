package com.microtomcat.session.distributed;

import com.microtomcat.session.Session;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySessionStoreAdapter implements SessionStoreAdapter {
    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    @Override
    public void saveSession(Session session) {
        if (session != null) {
            sessions.put(session.getId(), session);
        }
    }

    @Override
    public Session loadSession(String sessionId) {
        if (sessionId != null) {
            Session session = sessions.get(sessionId);
            if (session != null && !session.isValid()) {
                deleteSession(sessionId);
                return null;
            }
            return session;
        }
        return null;
    }

    @Override
    public void deleteSession(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }
} 