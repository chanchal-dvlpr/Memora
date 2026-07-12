package com.contextengine.domain.entity;

import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.WorkspaceId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Models the actual file system structure, directory topologies, configurations,
 * and git-history states contained within the Project boundaries.
 */
public class Workspace {
    
    private final WorkspaceId id;
    private final ProjectId projectId;
    private final List<Path> trackedPaths;
    private String activeBranch;
    private String activeCommitHash;

    /**
     * Constructs a Workspace.
     *
     * @param id the unique workspace ID
     * @param projectId the parent project ID
     */
    public Workspace(WorkspaceId id, ProjectId projectId) {
        this.id = Objects.requireNonNull(id, "WorkspaceId must not be null");
        this.projectId = Objects.requireNonNull(projectId, "ProjectId must not be null");
        this.trackedPaths = new ArrayList<>();
        this.activeBranch = "";
        this.activeCommitHash = "";
    }

    public WorkspaceId id() {
        return id;
    }

    public ProjectId projectId() {
        return projectId;
    }

    public List<Path> trackedPaths() {
        return Collections.unmodifiableList(trackedPaths);
    }

    public String activeBranch() {
        return activeBranch;
    }

    public String activeCommitHash() {
        return activeCommitHash;
    }

    /**
     * Adds a file path to the list of tracked paths within the workspace.
     *
     * @param path the path to track
     */
    public void trackPath(Path path) {
        Objects.requireNonNull(path, "Path must not be null");
        if (!trackedPaths.contains(path)) {
            trackedPaths.add(path);
        }
    }

    /**
     * Removes a file path from the list of tracked paths within the workspace.
     *
     * @param path the path to untrack
     */
    public void untrackPath(Path path) {
        Objects.requireNonNull(path, "Path must not be null");
        trackedPaths.remove(path);
    }

    /**
     * Updates the active Git branch and commit hash metadata.
     *
     * @param branch the current git branch name
     * @param commitHash the current git commit hash
     */
    public void updateGitMetadata(String branch, String commitHash) {
        this.activeBranch = branch == null ? "" : branch.trim();
        this.activeCommitHash = commitHash == null ? "" : commitHash.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Workspace that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
