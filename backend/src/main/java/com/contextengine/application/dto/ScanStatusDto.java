package com.contextengine.application.dto;

/**
 * Data Transfer Object representing the current scan status and workspace metadata.
 */
public record ScanStatusDto(
    String projectId,
    boolean watcherActive,
    long filesProcessed,
    String activeBranch,
    String activeCommitHash
) {
}
