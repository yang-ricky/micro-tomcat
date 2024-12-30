package com.microtomcat.session;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SessionManager {
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    public SessionManager() {
        // 每分钟检查一次过期的会话
        scheduler.scheduleAtFixedRate(this::cleanExpiredSessions, 1, 1, TimeUnit.MINUTES);
    }

    public Session createSession() {
        String sessionId = generateSessionId();
        Session session = new Session(sessionId);
        sessions.put(sessionId, session);
        return session;
    }

    public Session getSession(String sessionId) {
        Session session = sessions.get(sessionId);
        if (session != null && session.isValid()) {
            session.access();
            return session;
        }
        return null;
    }

    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
    }

    protected String generateSessionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    protected void cleanExpiredSessions() {
        sessions.entrySet().removeIf(entry -> !entry.getValue().isValid());
    }

    public void shutdown() {
        scheduler.shutdown();
    }

    public void invalidateAll() {
        sessions.clear();
    }
} 