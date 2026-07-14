package com.contextengine.api.mapper;

import com.contextengine.api.response.ScanStatusResponse;
import com.contextengine.application.dto.ScanStatusDto;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Maps Scanner Application DTOs to REST response models.
 * <p>
 * Bounded Context: Presentation REST API
 * Reference: Section 5.5.3 (Codebase Index Engine)
 * </p>
 */
public final class ScanResponseMapper {

    private ScanResponseMapper() {
        // Prevent instantiation
    }

    /**
     * Transforms a ScanStatusDto to ScanStatusResponse.
     *
     * @param dto the application scanner status DTO payload
     * @return REST scan status response model
     */
    public static ScanStatusResponse toResponse(ScanStatusDto dto) {
        Objects.requireNonNull(dto, "ScanStatusDto must not be null");
        return new ScanStatusResponse(
            UUID.randomUUID().toString(),
            dto.projectId(),
            "COMPLETED",
            Instant.now().toString(),
            dto.watcherActive(),
            dto.filesProcessed()
        );
    }
}
