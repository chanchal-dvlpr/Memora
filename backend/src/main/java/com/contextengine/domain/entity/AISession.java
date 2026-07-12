package com.contextengine.domain.entity;

import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.SessionId;
import java.util.Objects;

/**
 * Coordinates a context-grounded conversation or session with an AI client.
 * Enforces zero persistent leakage by remaining purely in-memory in application usage.
 */
public class AISession {
    
    private final SessionId id;
    private final ProjectId projectId;
    private final String clientApp;
    private SessionState status;

    /**
     * Constructs an AISession in the HANDSHAKE state.
     *
     * @param id the unique session ID
     * @param projectId the associated project ID
     * @param clientApp the name/identifier of the consuming client application
     */
    public AISession(SessionId id, ProjectId projectId, String clientApp) {
        this.id = Objects.requireNonNull(id, "SessionId must not be null");
        this.projectId = Objects.requireNonNull(projectId, "ProjectId must not be null");
        
        if (clientApp == null || clientApp.trim().isEmpty()) {
            throw new IllegalArgumentException("Client application identifier must not be null or empty");
        }
        this.clientApp = clientApp.trim();
        this.status = SessionState.HANDSHAKE;
    }

    public SessionId id() {
        return id;
    }

    public ProjectId projectId() {
        return projectId;
    }

    public String clientApp() {
        return clientApp;
    }

    public SessionState status() {
        return status;
    }

    /**
     * Activates the session after successful handshake and validation.
     */
    public void activate() {
        if (this.status != SessionState.HANDSHAKE) {
            throw new IllegalStateException("Session must be in HANDSHAKE state to be activated");
        }
        this.status = SessionState.ACTIVE;
    }

    /**
     * Closes the session and marks it as terminated.
     */
    public void close() {
        this.status = SessionState.CLOSED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AISession aiSession)) return false;
        return id.equals(aiSession.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
