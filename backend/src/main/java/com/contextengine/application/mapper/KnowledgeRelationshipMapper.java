package com.contextengine.application.mapper;

import com.contextengine.application.dto.KnowledgeRelationshipDto;
import com.contextengine.domain.entity.KnowledgeRelationship;
import com.contextengine.domain.valueobject.GraphWeight;
import com.contextengine.domain.valueobject.NodeId;
import com.contextengine.domain.valueobject.RelationshipId;
import java.util.Objects;
import java.util.UUID;

/**
 * Maps KnowledgeRelationship domain entities to and from KnowledgeRelationshipDto representations.
 * <p>
 * Bounded Context: Knowledge Graph Representation
 * Related Domain Aggregate: KnowledgeRelationship
 * </p>
 */
public final class KnowledgeRelationshipMapper {

    private KnowledgeRelationshipMapper() {
        // Prevent instantiation
    }

    /**
     * Maps a KnowledgeRelationship domain entity to a KnowledgeRelationshipDto.
     *
     * @param rel the knowledge relationship domain entity
     * @return the mapped KnowledgeRelationshipDto
     */
    public static KnowledgeRelationshipDto toDto(KnowledgeRelationship rel) {
        Objects.requireNonNull(rel, "KnowledgeRelationship must not be null");
        return new KnowledgeRelationshipDto(
            rel.id().value().toString(),
            rel.sourceNodeId().value().toString(),
            rel.targetNodeId().value().toString(),
            rel.type(),
            rel.weight().value()
        );
    }

    /**
     * Maps a KnowledgeRelationshipDto to a KnowledgeRelationship domain entity.
     *
     * @param dto the knowledge relationship DTO
     * @return the mapped KnowledgeRelationship domain entity
     */
    public static KnowledgeRelationship toDomain(KnowledgeRelationshipDto dto) {
        Objects.requireNonNull(dto, "KnowledgeRelationshipDto must not be null");
        return new KnowledgeRelationship(
            new RelationshipId(UUID.fromString(dto.id())),
            new NodeId(UUID.fromString(dto.sourceNodeId())),
            new NodeId(UUID.fromString(dto.targetNodeId())),
            dto.type(),
            new GraphWeight(dto.weight())
        );
    }
}
