package com.contextengine.domain.entity;

import com.contextengine.domain.valueobject.FeatureId;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.Priority;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.TaskId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a granular, atomic unit of development work.
 */
public class Task {
    
    private final TaskId id;
    private final FeatureId featureId; // optional, can be null
    private final ProjectId projectId;
    private final String description;
    private final Priority priority;
    private TaskState status;
    private final List<Path> linkedFiles;
    private final List<TaskId> dependencyTaskIds;

    /**
     * Constructs a Task.
     * Starts in READY if dependencyTaskIds is empty, otherwise BLOCKED.
     *
     * @param id the unique task ID
     * @param featureId the parent feature ID (nullable)
     * @param projectId the parent project ID
     * @param description the description of the task
     * @param priority the task priority
     * @param dependencyTaskIds list of prerequisite task IDs
     */
    public Task(TaskId id, FeatureId featureId, ProjectId projectId, String description, Priority priority, List<TaskId> dependencyTaskIds) {
        this.id = Objects.requireNonNull(id, "TaskId must not be null");
        this.projectId = Objects.requireNonNull(projectId, "ProjectId must not be null");
        this.priority = Objects.requireNonNull(priority, "Priority must not be null");
        this.featureId = featureId; // can be null
        
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Task description must not be null or empty");
        }
        this.description = description.trim();
        this.dependencyTaskIds = dependencyTaskIds == null ? new ArrayList<>() : new ArrayList<>(dependencyTaskIds);
        this.status = this.dependencyTaskIds.isEmpty() ? TaskState.READY : TaskState.BLOCKED;
        this.linkedFiles = new ArrayList<>();
    }

    public TaskId id() {
        return id;
    }

    public FeatureId featureId() {
        return featureId;
    }

    public ProjectId projectId() {
        return projectId;
    }

    public String description() {
        return description;
    }

    public Priority priority() {
        return priority;
    }

    public TaskState status() {
        return status;
    }

    public List<Path> linkedFiles() {
        return Collections.unmodifiableList(linkedFiles);
    }

    public List<TaskId> dependencyTaskIds() {
        return Collections.unmodifiableList(dependencyTaskIds);
    }

    /**
     * Adds a file path linked to this task.
     *
     * @param path the file path to link
     */
    public void linkFile(Path path) {
        Objects.requireNonNull(path, "Path must not be null");
        if (!linkedFiles.contains(path)) {
            linkedFiles.add(path);
        }
    }

    /**
     * Moves task from BLOCKED to READY after verifying dependencies are resolved.
     */
    public void resolveDependencies() {
        if (this.status != TaskState.BLOCKED) {
            throw new IllegalStateException("Task must be BLOCKED to resolve dependencies");
        }
        this.status = TaskState.READY;
    }

    /**
     * Activates task from READY to IN_PROGRESS.
     */
    public void startExecution() {
        if (this.status != TaskState.READY) {
            throw new IllegalStateException("Task must be READY to start execution. Current state: " + this.status);
        }
        this.status = TaskState.IN_PROGRESS;
    }

    /**
     * Completes task from IN_PROGRESS to COMPLETED.
     */
    public void complete() {
        if (this.status != TaskState.IN_PROGRESS) {
            throw new IllegalStateException("Task must be IN_PROGRESS to be completed. Current state: " + this.status);
        }
        this.status = TaskState.COMPLETED;
    }

    /**
     * Manually blocks task.
     */
    public void block() {
        this.status = TaskState.BLOCKED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task task)) return false;
        return id.equals(task.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
