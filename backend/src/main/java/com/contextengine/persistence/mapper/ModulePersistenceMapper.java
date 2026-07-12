package com.contextengine.persistence.mapper;

import com.contextengine.domain.entity.Module;
import com.contextengine.domain.valueobject.ModuleId;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.persistence.entity.ModuleEntity;
import com.contextengine.persistence.entity.ProjectEntity;
import java.util.UUID;

/**
 * Persistence mapper converting between Module domain model and ModuleEntity.
 * <p>
 * Bounded Context: Project Management
 * Related Subsystem: Persistent Storage
 * </p>
 */
public class ModulePersistenceMapper {

    /**
     * Converts Module domain model to ModuleEntity.
     *
     * @param domain domain model
     * @param projectEntity associated project entity
     * @return module entity
     */
    public ModuleEntity toEntity(Module domain, ProjectEntity projectEntity) {
        if (domain == null) return null;
        ModuleEntity entity = new ModuleEntity();
        entity.setId(domain.id().value().toString());
        entity.setProject(projectEntity);
        entity.setName(domain.name());
        entity.setRelativePath(domain.relativePath().value());
        return entity;
    }

    /**
     * Converts ModuleEntity to Module domain model.
     *
     * @param entity module entity
     * @param projectId parent project ID
     * @return module domain model
     */
    public Module toDomain(ModuleEntity entity, ProjectId projectId) {
        if (entity == null) return null;
        return new Module(
            new ModuleId(UUID.fromString(entity.getId())),
            projectId,
            entity.getName(),
            new Path(entity.getRelativePath())
        );
    }
}
