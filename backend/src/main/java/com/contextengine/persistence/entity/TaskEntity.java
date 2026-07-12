package com.contextengine.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * JPA entity representing a Project Task.
 * <p>
 * Bounded Context: Project Management
 * Related Subsystem: Persistent Storage
 * </p>
 */
@Entity
@Table(name = "tasks")
public class TaskEntity extends BasePersistenceEntity {

    @Id
    @Column(name = "task_id", length = 36, nullable = false)
    private String id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Column(name = "feature_id", length = 36)
    private String featureId;

    @Column(name = "description", length = 2048, nullable = false)
    private String description;

    @Column(name = "priority", length = 32, nullable = false)
    private String priority;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "linked_files", length = 4096)
    private String linkedFilesSerialized; // Comma-separated paths

    @Column(name = "dependency_task_ids", length = 4096)
    private String dependencyTaskIdsSerialized; // Comma-separated task IDs

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ProjectEntity getProject() {
        return project;
    }

    public void setProject(ProjectEntity project) {
        this.project = project;
    }

    public String getFeatureId() {
        return featureId;
    }

    public void setFeatureId(String featureId) {
        this.featureId = featureId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLinkedFilesSerialized() {
        return linkedFilesSerialized;
    }

    public void setLinkedFilesSerialized(String linkedFilesSerialized) {
        this.linkedFilesSerialized = linkedFilesSerialized;
    }

    public String getDependencyTaskIdsSerialized() {
        return dependencyTaskIdsSerialized;
    }

    public void setDependencyTaskIdsSerialized(String dependencyTaskIdsSerialized) {
        this.dependencyTaskIdsSerialized = dependencyTaskIdsSerialized;
    }
}
