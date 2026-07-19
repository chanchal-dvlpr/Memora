package com.contextengine.api.response;

/**
 * REST response model matching the exact format expected by the CLI for context operations.
 */
public record ContextCliResponse(
    String projectId,
    String content,
    String updatedAt
) {}
