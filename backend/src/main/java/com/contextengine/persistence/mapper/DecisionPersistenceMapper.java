package com.contextengine.persistence.mapper;

import com.contextengine.domain.entity.Decision;
import com.contextengine.domain.entity.DecisionState;
import com.contextengine.domain.valueobject.DecisionId;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.persistence.entity.DecisionEntity;
import com.contextengine.persistence.entity.ProjectEntity;
import java.util.UUID;

/**
 * Persistence mapper converting between Decision domain model and DecisionEntity.
 * <p>
 * Bounded Context: Project Management
 * Related Subsystem: Persistent Storage
 * </p>
 */
public class DecisionPersistenceMapper {

    /**
     * Converts Decision domain model to DecisionEntity.
     *
     * @param domain domain model
     * @param projectEntity associated project entity
     * @return decision entity
     */
    public DecisionEntity toEntity(Decision domain, ProjectEntity projectEntity) {
        if (domain == null) return null;
        DecisionEntity entity = new DecisionEntity();
        entity.setId(domain.id().value().toString());
        entity.setProject(projectEntity);
        entity.setTitle(domain.title());
        entity.setMarkdownPath(domain.markdownPath().value());
        entity.setStatus(domain.status().name());
        entity.setSupersededById(domain.supersededBy() != null ? domain.supersededBy().value().toString() : null);
        return entity;
    }

    /**
     * Converts DecisionEntity to Decision domain model.
     *
     * @param entity decision entity
     * @param projectId parent project ID
     * @return decision domain model
     */
    public Decision toDomain(DecisionEntity entity, ProjectId projectId) {
        if (entity == null) return null;
        return Decision.reconstruct(
            new DecisionId(UUID.fromString(entity.getId())),
            projectId,
            entity.getTitle(),
            new Path(entity.getMarkdownPath()),
            DecisionState.valueOf(entity.getStatus()),
            entity.getSupersededById() != null ? new DecisionId(UUID.fromString(entity.getSupersededById())) : null
        );
    }
}
