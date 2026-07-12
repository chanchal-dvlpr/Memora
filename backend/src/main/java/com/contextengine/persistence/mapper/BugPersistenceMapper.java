package com.contextengine.persistence.mapper;

import com.contextengine.domain.entity.Bug;
import com.contextengine.domain.entity.BugState;
import com.contextengine.domain.valueobject.BugId;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.persistence.entity.BugEntity;
import com.contextengine.persistence.entity.ProjectEntity;
import java.util.UUID;

/**
 * Persistence mapper converting between Bug domain model and BugEntity.
 * <p>
 * Bounded Context: Project Management
 * Related Subsystem: Persistent Storage
 * </p>
 */
public class BugPersistenceMapper {

    /**
     * Converts Bug domain model to BugEntity.
     *
     * @param domain domain model
     * @param projectEntity associated project entity
     * @return bug entity
     */
    public BugEntity toEntity(Bug domain, ProjectEntity projectEntity) {
        if (domain == null) return null;
        BugEntity entity = new BugEntity();
        entity.setId(domain.id().value().toString());
        entity.setProject(projectEntity);
        entity.setFilePath(domain.filePath() != null ? domain.filePath().value() : null);
        entity.setLineStart(domain.lineStart());
        entity.setLineEnd(domain.lineEnd());
        entity.setCommitHash(domain.commitHash());
        entity.setStatus(domain.status().name());
        return entity;
    }

    /**
     * Converts BugEntity to Bug domain model.
     *
     * @param entity bug entity
     * @param projectId parent project ID
     * @return bug domain model
     */
    public Bug toDomain(BugEntity entity, ProjectId projectId) {
        if (entity == null) return null;
        return Bug.reconstruct(
            new BugId(UUID.fromString(entity.getId())),
            projectId,
            entity.getFilePath() != null ? new Path(entity.getFilePath()) : null,
            entity.getLineStart(),
            entity.getLineEnd(),
            entity.getCommitHash(),
            BugState.valueOf(entity.getStatus())
        );
    }
}
