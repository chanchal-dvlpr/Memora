package com.contextengine.application.mapper;

import com.contextengine.application.dto.TaskDto;
import com.contextengine.domain.entity.Task;
import com.contextengine.domain.entity.TaskState;
import com.contextengine.domain.valueobject.FeatureId;
import com.contextengine.domain.valueobject.Priority;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.TaskId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Maps Task domain entities to and from TaskDto representations.
 * <p>
 * Bounded Context: Project Management
 * Related Domain Aggregate: Task
 * </p>
 */
public final class TaskMapper {

    private TaskMapper() {
        // Prevent instantiation
    }

    /**
     * Maps a Task domain entity to a TaskDto.
     *
     * @param task the task domain entity
     * @return the mapped TaskDto
     */
    public static TaskDto toDto(Task task) {
        Objects.requireNonNull(task, "Task must not be null");
        return new TaskDto(
            task.id().value().toString(),
            task.projectId().value().toString(),
            task.featureId() != null ? task.featureId().value().toString() : null,
            task.description(),
            task.status().name(),
            task.priority().name()
        );
    }

    /**
     * Maps a TaskDto to a Task domain entity using the domain reconstruction factory.
     *
     * @param dto the task DTO
     * @return the mapped Task domain entity
     */
    public static Task toDomain(TaskDto dto) {
        Objects.requireNonNull(dto, "TaskDto must not be null");
        return Task.reconstruct(
            new TaskId(UUID.fromString(dto.id())),
            dto.featureId() != null ? new FeatureId(UUID.fromString(dto.featureId())) : null,
            new ProjectId(UUID.fromString(dto.projectId())),
            dto.description(),
            Priority.valueOf(dto.priority()),
            TaskState.valueOf(dto.status()),
            List.of(),
            List.of()
        );
    }
}
