package com.contextengine.application.mapper;

import com.contextengine.application.dto.ContextSnapshotDto;
import com.contextengine.domain.entity.ContextSnapshot;
import com.contextengine.domain.valueobject.*;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Maps ContextSnapshot domain entities to and from ContextSnapshotDto representations.
 * <p>
 * Bounded Context: Context Assembly
 * Related Domain Aggregate: ContextSnapshot
 * </p>
 */
public final class ContextSnapshotMapper {

    private ContextSnapshotMapper() {
        // Prevent instantiation
    }

    /**
     * Maps a ContextSnapshot domain entity to a ContextSnapshotDto.
     *
     * @param snapshot the context snapshot domain entity
     * @return the mapped ContextSnapshotDto
     */
    public static ContextSnapshotDto toDto(ContextSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "ContextSnapshot must not be null");
        return new ContextSnapshotDto(
            snapshot.id().value().toString(),
            snapshot.projectId().value().toString(),
            "", // Payload is generated dynamically by ContextGenerationService
            snapshot.summary().tokenFootprint(),
            snapshot.createdAt().value().toString()
        );
    }

    /**
     * Maps a ContextSnapshotDto to a ContextSnapshot domain entity.
     *
     * @param dto the context snapshot DTO
     * @return the mapped ContextSnapshot domain entity
     */
    public static ContextSnapshot toDomain(ContextSnapshotDto dto) {
        Objects.requireNonNull(dto, "ContextSnapshotDto must not be null");
        return new ContextSnapshot(
            new SnapshotId(UUID.fromString(dto.id())),
            new ProjectId(UUID.fromString(dto.projectId())),
            new Version(1),
            new Timestamp(Instant.parse(dto.timestamp())),
            new ContextSummary(0, dto.tokensUsed(), List.of()),
            List.of()
        );
    }
}
