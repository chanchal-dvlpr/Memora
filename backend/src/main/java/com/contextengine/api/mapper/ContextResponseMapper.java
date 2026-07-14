package com.contextengine.api.mapper;

import com.contextengine.api.response.ContextResponse;
import com.contextengine.application.dto.ContextSnapshotDto;
import java.util.Objects;

/**
 * Maps Context Assembly DTOs to REST response models.
 * <p>
 * Bounded Context: Presentation REST API
 * Reference: Section 5.5.5 (Context Assembly Engine)
 * </p>
 */
public final class ContextResponseMapper {

    private ContextResponseMapper() {
        // Prevent instantiation
    }

    /**
     * Transforms a ContextSnapshotDto to ContextResponse.
     *
     * @param dto the application context snapshot DTO payload
     * @return REST context response model
     */
    public static ContextResponse toResponse(ContextSnapshotDto dto) {
        Objects.requireNonNull(dto, "ContextSnapshotDto must not be null");
        return new ContextResponse(
            dto.id(),
            dto.projectId(),
            dto.tokensUsed(),
            dto.payload(),
            dto.timestamp()
        );
    }
}
