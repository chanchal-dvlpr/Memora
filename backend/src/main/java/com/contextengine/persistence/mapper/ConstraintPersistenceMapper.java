package com.contextengine.persistence.mapper;

import com.contextengine.domain.entity.Constraint;
import com.contextengine.domain.valueobject.ConstraintId;
import com.contextengine.domain.valueobject.Metadata;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.persistence.entity.ConstraintEntity;
import com.contextengine.persistence.entity.ProjectEntity;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Persistence mapper converting between Constraint domain model and ConstraintEntity.
 * <p>
 * Bounded Context: Project Management
 * Related Subsystem: Persistent Storage
 * </p>
 */
public class ConstraintPersistenceMapper {

    /**
     * Converts Constraint domain model to ConstraintEntity.
     *
     * @param domain domain model
     * @param projectEntity associated project entity
     * @return constraint entity
     */
    public ConstraintEntity toEntity(Constraint domain, ProjectEntity projectEntity) {
        if (domain == null) return null;
        ConstraintEntity entity = new ConstraintEntity();
        entity.setId(domain.id().value().toString());
        entity.setProject(projectEntity);
        entity.setConstraintType(domain.constraintType());
        entity.setRulesSerialized(serializeMetadata(domain.rules()));
        return entity;
    }

    /**
     * Converts ConstraintEntity to Constraint domain model.
     *
     * @param entity constraint entity
     * @param projectId parent project ID
     * @return constraint domain model
     */
    public Constraint toDomain(ConstraintEntity entity, ProjectId projectId) {
        if (entity == null) return null;
        return new Constraint(
            new ConstraintId(UUID.fromString(entity.getId())),
            projectId,
            entity.getConstraintType(),
            deserializeMetadata(entity.getRulesSerialized())
        );
    }

    private String serializeMetadata(Metadata metadata) {
        if (metadata == null || metadata.values().isEmpty()) {
            return "";
        }
        return metadata.values().entrySet().stream()
            .map(entry -> entry.getKey() + ":" + entry.getValue())
            .collect(Collectors.joining(","));
    }

    private Metadata deserializeMetadata(String serialized) {
        if (serialized == null || serialized.trim().isEmpty()) {
            return new Metadata(Collections.emptyMap());
        }
        Map<String, String> map = new HashMap<>();
        String[] parts = serialized.split(",");
        for (String part : parts) {
            String[] fields = part.split(":");
            if (fields.length == 2) {
                map.put(fields[0], fields[1]);
            }
        }
        return new Metadata(map);
    }
}
