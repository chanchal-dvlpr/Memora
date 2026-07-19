package com.contextengine.api.response;

/**
 * REST response model representing the outcome of a project scan refresh.
 */
public record RefreshProjectResponse(
    String projectId,
    long filesScanned,
    boolean snapshotGenerated
) {}
