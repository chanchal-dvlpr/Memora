package com.contextengine.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * JPA entity representing a directed relationship edge inside the Knowledge Graph.
 * <p>
 * Bounded Context: Knowledge Graph
 * Related Subsystem: Persistent Storage
 * </p>
 */
@Entity
@Table(
    name = "knowledge_relationships",
    indexes = {
        @Index(name = "idx_edge_project_src_type", columnList = "project_id, source_node_id, relationship_type"),
        @Index(name = "idx_edge_project_tgt_type", columnList = "project_id, target_node_id, relationship_type")
    }
)
public class KnowledgeRelationshipEntity extends BasePersistenceEntity {

    @Id
    @Column(name = "edge_id", length = 36, nullable = false)
    private String id;

    @Column(name = "project_id", length = 36, nullable = false)
    private String projectId;

    @Column(name = "source_node_id", length = 36, nullable = false)
    private String sourceNodeId;

    @Column(name = "target_node_id", length = 36, nullable = false)
    private String targetNodeId;

    @Column(name = "relationship_type", length = 64, nullable = false)
    private String relationshipType;

    @Column(name = "call_frequency", nullable = false)
    private int callFrequency;

    @Column(name = "edge_properties", length = 32768)
    private String edgeProperties;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getSourceNodeId() {
        return sourceNodeId;
    }

    public void setSourceNodeId(String sourceNodeId) {
        this.sourceNodeId = sourceNodeId;
    }

    public String getTargetNodeId() {
        return targetNodeId;
    }

    public void setTargetNodeId(String targetNodeId) {
        this.targetNodeId = targetNodeId;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
    }

    public int getCallFrequency() {
        return callFrequency;
    }

    public void setCallFrequency(int callFrequency) {
        this.callFrequency = callFrequency;
    }

    public String getEdgeProperties() {
        return edgeProperties;
    }

    public void setEdgeProperties(String edgeProperties) {
        this.edgeProperties = edgeProperties;
    }
}
