package com.contextengine.persistence.mapper;

import com.contextengine.domain.entity.AISession;
import com.contextengine.domain.entity.SessionState;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.SessionId;
import com.contextengine.persistence.entity.AISessionEntity;
import java.util.UUID;

/**
 * Persistence mapper converting between AISession domain model and AISessionEntity.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Persistent Storage
 * </p>
 */
public class AISessionPersistenceMapper {

    /**
     * Converts AISession domain model to AISessionEntity.
     *
     * @param domain domain model
     * @return session entity
     */
    public AISessionEntity toEntity(AISession domain) {
        if (domain == null) return null;
        AISessionEntity entity = new AISessionEntity();
        entity.setId(domain.id().value().toString());
        entity.setProjectId(domain.projectId().value().toString());
        entity.setClientApp(domain.clientApp());
        entity.setStatus(domain.status().name());
        return entity;
    }

    /**
     * Converts AISessionEntity to AISession domain model.
     *
     * @param entity session entity
     * @return session domain model
     */
    public AISession toDomain(AISessionEntity entity) {
        if (entity == null) return null;
        return AISession.reconstruct(
            new SessionId(UUID.fromString(entity.getId())),
            new ProjectId(UUID.fromString(entity.getProjectId())),
            entity.getClientApp(),
            SessionState.valueOf(entity.getStatus())
        );
    }
}
