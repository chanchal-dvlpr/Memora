package com.contextengine.domain.repository;

import com.contextengine.domain.entity.AISession;
import com.contextengine.domain.valueobject.SessionId;
import java.util.Collection;
import java.util.Optional;

/**
 * Manages session isolations and memory buffers for active connection sessions.
 */
public interface AISessionRepository {
    
    /**
     * Persists conversational history structures, active filters, and session context properties.
     *
     * @param session the AI session aggregate root
     */
    void save(AISession session);
    
    /**
     * Resolves an active conversational interface tracking structure by ID.
     *
     * @param sessionId the session ID
     * @return an optional containing the active session, or empty if not found
     */
    Optional<AISession> findById(SessionId sessionId);
    
    /**
     * Groups active conversational sessions associated with a specific tool or IDE host.
     *
     * @param clientApp the AIAssistant/client application name
     * @return collection of matching AI sessions
     */
    Collection<AISession> findByClientIdentity(String clientApp);
    
    /**
     * Purges session memory pools upon connection termination.
     *
     * @param sessionId the session ID to delete
     */
    void remove(SessionId sessionId);
}
