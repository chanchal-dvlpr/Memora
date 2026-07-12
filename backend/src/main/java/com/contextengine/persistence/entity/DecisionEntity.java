package com.contextengine.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * JPA entity representing a Project Decision record.
 * <p>
 * Bounded Context: Project Management
 * Related Subsystem: Persistent Storage
 * </p>
 */
@Entity
@Table(name = "decisions")
public class DecisionEntity extends BasePersistenceEntity {

    @Id
    @Column(name = "decision_id", length = 36, nullable = false)
    private String id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "markdown_path", length = 1024, nullable = false)
    private String markdownPath;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "superseded_by_id", length = 36)
    private String supersededById;

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMarkdownPath() {
        return markdownPath;
    }

    public void setMarkdownPath(String markdownPath) {
        this.markdownPath = markdownPath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSupersededById() {
        return supersededById;
    }

    public void setSupersededById(String supersededById) {
        this.supersededById = supersededById;
    }
}
