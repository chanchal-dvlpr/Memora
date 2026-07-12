package com.contextengine.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * JPA entity representing a recorded system event envelope in the append-only Event Journal.
 * Maps directly to SCHEMA-EVENT database structure.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Persistent Storage
 * </p>
 */
@Entity
@Table(
    name = "system_events",
    uniqueConstraints = {
        @UniqueConstraint(name = "uc_event_sequence", columnNames = {"sequence_num"})
    },
    indexes = {
        @Index(name = "idx_event_project_seq", columnList = "project_id, sequence_num")
    }
)
public class EventEntity extends BasePersistenceEntity {

    @Id
    @Column(name = "event_id", length = 36, nullable = false)
    private String id;

    @Column(name = "project_id", length = 36)
    private String projectId;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Column(name = "payload", length = 1048576, nullable = false) // TEXT - Max 1MB
    private String payload;

    @Column(name = "sequence_num", nullable = false)
    private Long sequenceNum;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "correlation_id", length = 36, nullable = false)
    private String correlationId;

    @Column(name = "causation_id", length = 36, nullable = false)
    private String causationId;

    @Column(name = "version", nullable = false)
    private Integer version;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Long getSequenceNum() {
        return sequenceNum;
    }

    public void setSequenceNum(Long sequenceNum) {
        this.sequenceNum = sequenceNum;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getCausationId() {
        return causationId;
    }

    public void setCausationId(String causationId) {
        this.causationId = causationId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
