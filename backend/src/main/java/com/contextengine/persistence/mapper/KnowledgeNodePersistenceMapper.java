package com.contextengine.persistence.mapper;

import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.valueobject.Metadata;
import com.contextengine.domain.valueobject.NodeId;
import com.contextengine.persistence.entity.KnowledgeNodeEntity;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Persistence mapper converting between KnowledgeNode domain model and KnowledgeNodeEntity.
 * <p>
 * Bounded Context: Knowledge Graph
 * Related Subsystem: Persistent Storage
 * </p>
 */
public class KnowledgeNodePersistenceMapper {

    /**
     * Converts KnowledgeNode domain model to KnowledgeNodeEntity.
     *
     * @param domain domain model
     * @param projectId associated project ID
     * @return knowledge node entity
     */
    public KnowledgeNodeEntity toEntity(KnowledgeNode domain, String projectId) {
        if (domain == null) return null;
        KnowledgeNodeEntity entity = new KnowledgeNodeEntity();
        entity.setId(domain.id().value().toString());
        entity.setProjectId(projectId);
        entity.setSymbolType(domain.type());

        Map<String, String> attrs = domain.attributes().values();
        entity.setQualifiedName(attrs.getOrDefault("qualifiedName", attrs.getOrDefault("name", "")));
        entity.setFileId(attrs.get("fileId"));
        entity.setSourceRange(attrs.get("sourceRange"));
        entity.setDocstring(attrs.get("docstring"));
        entity.setSymbolSignature(attrs.get("symbolSignature"));
        entity.setNodeProperties(serializeMap(attrs));

        return entity;
    }

    /**
     * Converts KnowledgeNodeEntity to KnowledgeNode domain model.
     *
     * @param entity knowledge node entity
     * @return knowledge node domain model
     */
    public KnowledgeNode toDomain(KnowledgeNodeEntity entity) {
        if (entity == null) return null;
        return new KnowledgeNode(
            new NodeId(UUID.fromString(entity.getId())),
            entity.getSymbolType(),
            new Metadata(deserializeMap(entity.getNodeProperties()))
        );
    }

    private String serializeMap(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }
        return map.entrySet().stream()
            .map(entry -> entry.getKey() + ":" + entry.getValue())
            .collect(Collectors.joining(","));
    }

    private Map<String, String> deserializeMap(String serialized) {
        if (serialized == null || serialized.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new HashMap<>();
        String[] parts = serialized.split(",");
        for (String part : parts) {
            String[] fields = part.split(":");
            if (fields.length == 2) {
                map.put(fields[0], fields[1]);
            }
        }
        return map;
    }
}
