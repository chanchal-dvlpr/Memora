package com.contextengine.application.mapper;

import com.contextengine.application.dto.KnowledgeNodeDto;
import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.valueobject.Metadata;
import com.contextengine.domain.valueobject.NodeId;
import java.util.Objects;
import java.util.UUID;

/**
 * Maps KnowledgeNode domain entities to and from KnowledgeNodeDto representations.
 * <p>
 * Bounded Context: Knowledge Graph Representation
 * Related Domain Aggregate: KnowledgeNode
 * </p>
 */
public final class KnowledgeNodeMapper {

    private KnowledgeNodeMapper() {
        // Prevent instantiation
    }

    /**
     * Maps a KnowledgeNode domain entity to a KnowledgeNodeDto.
     *
     * @param node the knowledge node domain entity
     * @return the mapped KnowledgeNodeDto
     */
    public static KnowledgeNodeDto toDto(KnowledgeNode node) {
        Objects.requireNonNull(node, "KnowledgeNode must not be null");
        return new KnowledgeNodeDto(
            node.id().value().toString(),
            node.type(),
            node.attributes().values()
        );
    }

    /**
     * Maps a KnowledgeNodeDto to a KnowledgeNode domain entity.
     *
     * @param dto the knowledge node DTO
     * @return the mapped KnowledgeNode domain entity
     */
    public static KnowledgeNode toDomain(KnowledgeNodeDto dto) {
        Objects.requireNonNull(dto, "KnowledgeNodeDto must not be null");
        return new KnowledgeNode(
            new NodeId(UUID.fromString(dto.id())),
            dto.type(),
            new Metadata(dto.attributes())
        );
    }
}
