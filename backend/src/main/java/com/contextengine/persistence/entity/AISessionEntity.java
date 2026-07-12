package com.contextengine.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity representing an active AI Conversational Session.
 * <p>
 * Bounded Context: AI Ingestion
 * Related Subsystem: Persistent Storage
 * </p>
 */
@Entity
@Table(name = "ai_sessions")
public class AISessionEntity extends BasePersistenceEntity {

    @Id
    @Column(name = "session_id", length = 36, nullable = false)
    private String id;

    @Column(name = "project_id", length = 36, nullable = false)
    private String projectId;

    @Column(name = "client_app", length = 255, nullable = false)
    private String clientApp;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getClientApp() {
        return clientApp;
    }

    public void setClientApp(String clientApp) {
        this.clientApp = clientApp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
