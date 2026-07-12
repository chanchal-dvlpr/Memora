package com.contextengine.application.dto;

import java.util.List;

/**
 * Data Transfer Object representing a Project Workspace.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Domain Aggregate: Workspace
 * </p>
 *
 * @param id the unique workspace identifier
 * @param projectId the associated parent project identifier
 * @param trackedPaths list of relative file paths currently tracked in this workspace
 * @param activeBranch the active Git branch name, or empty if not under version control
 * @param activeCommitHash the active Git commit hash, or empty if not under version control
 */
public record WorkspaceDto(
    String id,
    String projectId,
    List<String> trackedPaths,
    String activeBranch,
    String activeCommitHash
) {
    /**
     * Constructs a WorkspaceDto. Performs defensive copying of the trackedPaths list.
     */
    public WorkspaceDto {
        trackedPaths = trackedPaths != null ? List.copyOf(trackedPaths) : List.of();
    }
}
