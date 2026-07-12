package com.contextengine.persistence.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity representing the Workspace state mapping.
 * <p>
 * Bounded Context: Project Management
 * Related Subsystem: Persistent Storage
 * </p>
 */
@Entity
@Table(name = "workspaces")
public class WorkspaceEntity extends BasePersistenceEntity {

    @Id
    @Column(name = "workspace_id", length = 36, nullable = false)
    private String id;

    @OneToOne
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "workspace_tracked_paths", joinColumns = @JoinColumn(name = "workspace_id"))
    @Column(name = "path", length = 1024, nullable = false)
    private List<String> trackedPaths = new ArrayList<>();

    @Column(name = "active_branch", length = 255, nullable = false)
    private String activeBranch;

    @Column(name = "active_commit_hash", length = 255, nullable = false)
    private String activeCommitHash;

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

    public List<String> getTrackedPaths() {
        return trackedPaths;
    }

    public void setTrackedPaths(List<String> trackedPaths) {
        this.trackedPaths = trackedPaths;
    }

    public String getActiveBranch() {
        return activeBranch;
    }

    public void setActiveBranch(String activeBranch) {
        this.activeBranch = activeBranch;
    }

    public String getActiveCommitHash() {
        return activeCommitHash;
    }

    public void setActiveCommitHash(String activeCommitHash) {
        this.activeCommitHash = activeCommitHash;
    }
}
