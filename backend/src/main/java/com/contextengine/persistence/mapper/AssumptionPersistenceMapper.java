package com.contextengine.persistence.mapper;

import com.contextengine.domain.entity.Assumption;
import com.contextengine.domain.entity.AssumptionState;
import com.contextengine.domain.valueobject.AssumptionId;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.persistence.entity.AssumptionEntity;
import com.contextengine.persistence.entity.ProjectEntity;
import java.util.Objects;
import java.util.UUID;

/**
 * Persistence mapper converting between Assumption domain model and AssumptionEntity.
 * <p>
 * Bounded Context: Project Management
 * Related Subsystem: Persistent Storage
 * </p>
 */
public class AssumptionPersistenceMapper {

    /**
     * Converts Assumption domain model to AssumptionEntity.
     *
     * @param domain domain model
     * @param projectEntity associated project entity
     * @return assumption entity
     */
    public AssumptionEntity toEntity(Assumption domain, ProjectEntity projectEntity) {
        if (domain == null) return null;
        AssumptionEntity entity = new AssumptionEntity();
        entity.setId(domain.id().value().toString());
        entity.setProject(projectEntity);
        entity.setStatement(domain.description());
        entity.setContext(domain.status().name());
        return entity;
    }

    /**
     * Converts AssumptionEntity to Assumption domain model.
     *
     * @param entity assumption entity
     * @param projectId parent project ID
     * @return assumption domain model
     */
    public Assumption toDomain(AssumptionEntity entity, ProjectId projectId) {
        if (entity == null) return null;
        return Assumption.reconstruct(
            new AssumptionId(UUID.fromString(entity.getId())),
            projectId,
            entity.getStatement(),
            AssumptionState.valueOf(entity.getContext())
        );
    }
}
