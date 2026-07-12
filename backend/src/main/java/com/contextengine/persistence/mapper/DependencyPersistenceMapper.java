package com.contextengine.persistence.mapper;

import com.contextengine.domain.entity.Dependency;
import com.contextengine.domain.valueobject.DependencyId;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.SemanticVersion;
import com.contextengine.persistence.entity.DependencyEntity;
import com.contextengine.persistence.entity.ProjectEntity;
import java.util.UUID;

/**
 * Persistence mapper converting between Dependency domain model and DependencyEntity.
 * <p>
 * Bounded Context: Project Management
 * Related Subsystem: Persistent Storage
 * </p>
 */
public class DependencyPersistenceMapper {

    /**
     * Converts Dependency domain model to DependencyEntity.
     *
     * @param domain domain model
     * @param projectEntity associated project entity
     * @return dependency entity
     */
    public DependencyEntity toEntity(Dependency domain, ProjectEntity projectEntity) {
        if (domain == null) return null;
        DependencyEntity entity = new DependencyEntity();
        entity.setId(domain.id().value().toString());
        entity.setProject(projectEntity);
        entity.setPackageName(domain.packageName());
        entity.setVersionNumber(domain.version().value());
        entity.setManifestPath(domain.manifestPath().value());
        return entity;
    }

    /**
     * Converts DependencyEntity to Dependency domain model.
     *
     * @param entity dependency entity
     * @param projectId parent project ID
     * @return dependency domain model
     */
    public Dependency toDomain(DependencyEntity entity, ProjectId projectId) {
        if (entity == null) return null;
        return new Dependency(
            new DependencyId(UUID.fromString(entity.getId())),
            projectId,
            entity.getPackageName(),
            new SemanticVersion(entity.getVersionNumber()),
            new Path(entity.getManifestPath())
        );
    }
}
