package com.microtomcat.session.distributed;

import com.microtomcat.session.Session;
import com.microtomcat.session.SessionManager;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletContext;
import com.microtomcat.session.StandardSession;

public class DistributedSessionManager extends SessionManager {
    private final SessionStoreAdapter sessionStore;
    private final ScheduledExecutorService scheduler;
    
    public DistributedSessionManager(ServletContext servletContext, SessionStoreAdapter sessionStore) {
        super(servletContext);
        this.sessionStore = sessionStore;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        // 每分钟检查过期会话
        scheduler.scheduleAtFixedRate(this::cleanExpiredSessions, 1, 1, TimeUnit.MINUTES);
    }
    
    @Override
    public Session createSession() {
        String sessionId = generateSessionId();
        Session session = new StandardSession(sessionId, servletContext);
        sessionStore.saveSession(session);
        return session;
    }
    
    @Override
    public Session getSession(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        
        Session session = sessionStore.loadSession(sessionId);
        if (session != null && session.isValid()) {
            session.access();
            sessionStore.saveSession(session);
            return session;
        }
        return null;
    }
    
    @Override
    public void removeSession(String sessionId) {
        if (sessionId != null) {
            sessionStore.deleteSession(sessionId);
        }
    }
    
    @Override
    protected String generateSessionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    @Override
    protected void cleanExpiredSessions() {
        // 这个方法可以留空，因为我们在loadSession时已经检查了过期
    }
    
    public SessionStoreAdapter getSessionStore() {
        return sessionStore;
    }
    
    @Override
    public void shutdown() {
        scheduler.shutdown();
        super.shutdown();
    }
} 