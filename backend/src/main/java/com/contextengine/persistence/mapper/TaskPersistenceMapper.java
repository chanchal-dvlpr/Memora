package com.contextengine.persistence.mapper;

import com.contextengine.domain.entity.Task;
import com.contextengine.domain.entity.TaskState;
import com.contextengine.domain.valueobject.FeatureId;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.Priority;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.TaskId;
import com.contextengine.persistence.entity.ProjectEntity;
import com.contextengine.persistence.entity.TaskEntity;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Persistence mapper converting between Task domain model and TaskEntity.
 * <p>
 * Bounded Context: Project Management
 * Related Subsystem: Persistent Storage
 * </p>
 */
public class TaskPersistenceMapper {

    /**
     * Converts Task domain model to TaskEntity.
     *
     * @param domain domain model
     * @param projectEntity associated project entity
     * @return task entity
     */
    public TaskEntity toEntity(Task domain, ProjectEntity projectEntity) {
        if (domain == null) return null;
        TaskEntity entity = new TaskEntity();
        entity.setId(domain.id().value().toString());
        entity.setProject(projectEntity);
        entity.setFeatureId(domain.featureId() != null ? domain.featureId().value().toString() : null);
        entity.setDescription(domain.description());
        entity.setPriority(domain.priority().name());
        entity.setStatus(domain.status().name());
        entity.setLinkedFilesSerialized(serializePaths(domain.linkedFiles()));
        entity.setDependencyTaskIdsSerialized(serializeTaskIds(domain.dependencyTaskIds()));
        return entity;
    }

    /**
     * Converts TaskEntity to Task domain model.
     *
     * @param entity task entity
     * @param projectId parent project ID
     * @return task domain model
     */
    public Task toDomain(TaskEntity entity, ProjectId projectId) {
        if (entity == null) return null;
        return Task.reconstruct(
            new TaskId(UUID.fromString(entity.getId())),
            entity.getFeatureId() != null ? new FeatureId(UUID.fromString(entity.getFeatureId())) : null,
            projectId,
            entity.getDescription(),
            Priority.valueOf(entity.getPriority()),
            TaskState.valueOf(entity.getStatus()),
            deserializeTaskIds(entity.getDependencyTaskIdsSerialized()),
            deserializePaths(entity.getLinkedFilesSerialized())
        );
    }

    private String serializePaths(List<Path> paths) {
        if (paths == null) return "";
        return paths.stream().map(Path::value).collect(Collectors.joining(","));
    }

    private List<Path> deserializePaths(String serialized) {
        if (serialized == null || serialized.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(serialized.split(","))
            .map(Path::new)
            .collect(Collectors.toList());
    }

    private String serializeTaskIds(List<TaskId> ids) {
        if (ids == null) return "";
        return ids.stream().map(id -> id.value().toString()).collect(Collectors.joining(","));
    }

    private List<TaskId> deserializeTaskIds(String serialized) {
        if (serialized == null || serialized.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(serialized.split(","))
            .map(val -> new TaskId(UUID.fromString(val)))
            .collect(Collectors.toList());
    }
}
