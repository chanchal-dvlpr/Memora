package com.contextengine.application.mapper;

import com.contextengine.application.dto.DecisionDto;
import com.contextengine.domain.entity.Decision;
import com.contextengine.domain.entity.DecisionState;
import com.contextengine.domain.valueobject.DecisionId;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import java.util.Objects;
import java.util.UUID;

/**
 * Maps Decision domain entities to and from DecisionDto representations.
 * <p>
 * Bounded Context: Project Management
 * Related Domain Aggregate: Decision
 * </p>
 */
public final class DecisionMapper {

    private DecisionMapper() {
        // Prevent instantiation
    }

    /**
     * Maps a Decision domain entity to a DecisionDto.
     *
     * @param decision the decision domain entity
     * @return the mapped DecisionDto
     */
    public static DecisionDto toDto(Decision decision) {
        Objects.requireNonNull(decision, "Decision must not be null");
        return new DecisionDto(
            decision.id().value().toString(),
            decision.projectId().value().toString(),
            decision.title(),
            decision.markdownPath().value(),
            decision.status().name(),
            decision.supersededBy() != null ? decision.supersededBy().value().toString() : null
        );
    }

    /**
     * Maps a DecisionDto to a Decision domain entity using the domain reconstruction factory.
     *
     * @param dto the decision DTO
     * @return the mapped Decision domain entity
     */
    public static Decision toDomain(DecisionDto dto) {
        Objects.requireNonNull(dto, "DecisionDto must not be null");
        return Decision.reconstruct(
            new DecisionId(UUID.fromString(dto.id())),
            new ProjectId(UUID.fromString(dto.projectId())),
            dto.title(),
            new Path(dto.markdownPath()),
            DecisionState.valueOf(dto.status()),
            dto.supersededBy() != null ? new DecisionId(UUID.fromString(dto.supersededBy())) : null
        );
    }
}
