package com.contextengine.persistence.mapper;

import com.contextengine.domain.entity.KnowledgeRelationship;
import com.contextengine.domain.valueobject.GraphWeight;
import com.contextengine.domain.valueobject.NodeId;
import com.contextengine.domain.valueobject.RelationshipId;
import com.contextengine.persistence.entity.KnowledgeRelationshipEntity;
import java.util.UUID;

/**
 * Persistence mapper converting between KnowledgeRelationship domain model and KnowledgeRelationshipEntity.
 * <p>
 * Bounded Context: Knowledge Graph
 * Related Subsystem: Persistent Storage
 * </p>
 */
public class KnowledgeRelationshipPersistenceMapper {

    /**
     * Converts KnowledgeRelationship domain model to KnowledgeRelationshipEntity.
     *
     * @param domain domain model
     * @param projectId associated project ID
     * @return knowledge relationship entity
     */
    public KnowledgeRelationshipEntity toEntity(KnowledgeRelationship domain, String projectId) {
        if (domain == null) return null;
        KnowledgeRelationshipEntity entity = new KnowledgeRelationshipEntity();
        entity.setId(domain.id().value().toString());
        entity.setProjectId(projectId);
        entity.setSourceNodeId(domain.sourceNodeId().value().toString());
        entity.setTargetNodeId(domain.targetNodeId().value().toString());
        entity.setRelationshipType(domain.type());
        entity.setCallFrequency((int) Math.max(1.0, domain.weight().value()));
        entity.setEdgeProperties("weight:" + domain.weight().value());
        return entity;
    }

    /**
     * Converts KnowledgeRelationshipEntity to KnowledgeRelationship domain model.
     *
     * @param entity knowledge relationship entity
     * @return knowledge relationship domain model
     */
    public KnowledgeRelationship toDomain(KnowledgeRelationshipEntity entity) {
        if (entity == null) return null;
        double weightValue = 1.0;
        if (entity.getEdgeProperties() != null && entity.getEdgeProperties().startsWith("weight:")) {
            try {
                weightValue = Double.parseDouble(entity.getEdgeProperties().substring(7));
            } catch (NumberFormatException ignored) {}
        }
        return new KnowledgeRelationship(
            new RelationshipId(UUID.fromString(entity.getId())),
            new NodeId(UUID.fromString(entity.getSourceNodeId())),
            new NodeId(UUID.fromString(entity.getTargetNodeId())),
            entity.getRelationshipType(),
            new GraphWeight(weightValue)
        );
    }
}
