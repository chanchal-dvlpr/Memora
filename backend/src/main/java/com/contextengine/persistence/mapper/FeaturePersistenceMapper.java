package com.contextengine.persistence.mapper;

import com.contextengine.domain.entity.Feature;
import com.contextengine.domain.entity.FeatureState;
import com.contextengine.domain.valueobject.FeatureId;
import com.contextengine.domain.valueobject.Priority;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.persistence.entity.FeatureEntity;
import com.contextengine.persistence.entity.ProjectEntity;
import java.util.UUID;

/**
 * Persistence mapper converting between Feature domain model and FeatureEntity.
 * <p>
 * Bounded Context: Project Management
 * Related Subsystem: Persistent Storage
 * </p>
 */
public class FeaturePersistenceMapper {

    /**
     * Converts Feature domain model to FeatureEntity.
     *
     * @param domain domain model
     * @param projectEntity associated project entity
     * @return feature entity
     */
    public FeatureEntity toEntity(Feature domain, ProjectEntity projectEntity) {
        if (domain == null) return null;
        FeatureEntity entity = new FeatureEntity();
        entity.setId(domain.id().value().toString());
        entity.setProject(projectEntity);
        entity.setTitle(domain.title());
        entity.setPriority(domain.priority().name());
        entity.setStatus(domain.status().name());
        entity.setProgress(domain.progressPercentage());
        return entity;
    }

    /**
     * Converts FeatureEntity to Feature domain model.
     *
     * @param entity feature entity
     * @param projectId parent project ID
     * @return feature domain model
     */
    public Feature toDomain(FeatureEntity entity, ProjectId projectId) {
        if (entity == null) return null;
        return Feature.reconstruct(
            new FeatureId(UUID.fromString(entity.getId())),
            projectId,
            entity.getTitle(),
            Priority.valueOf(entity.getPriority()),
            FeatureState.valueOf(entity.getStatus()),
            entity.getProgress()
        );
    }
}
