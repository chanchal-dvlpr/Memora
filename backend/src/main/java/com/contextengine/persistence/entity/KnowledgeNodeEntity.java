package com.contextengine.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * JPA entity representing a codebase symbol node vertex inside the Knowledge Graph.
 * <p>
 * Bounded Context: Knowledge Graph
 * Related Subsystem: Persistent Storage
 * </p>
 */
@Entity
@Table(
    name = "knowledge_nodes",
    uniqueConstraints = {
        @UniqueConstraint(name = "uc_node_project_qname", columnNames = {"project_id", "qualified_name"})
    },
    indexes = {
        @Index(name = "idx_node_project_type", columnList = "project_id, symbol_type")
    }
)
public class KnowledgeNodeEntity extends BasePersistenceEntity {

    @Id
    @Column(name = "node_id", length = 36, nullable = false)
    private String id;

    @Column(name = "project_id", length = 36, nullable = false)
    private String projectId;

    @Column(name = "file_id", length = 36)
    private String fileId;

    @Column(name = "qualified_name", length = 512, nullable = false)
    private String qualifiedName;

    @Column(name = "symbol_type", length = 32, nullable = false)
    private String symbolType;

    @Column(name = "source_range", length = 1024)
    private String sourceRange;

    @Column(name = "docstring", length = 32768)
    private String docstring;

    @Column(name = "symbol_signature", length = 1024)
    private String symbolSignature;

    @Column(name = "node_properties", length = 32768)
    private String nodeProperties;

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

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public void setQualifiedName(String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }

    public String getSymbolType() {
        return symbolType;
    }

    public void setSymbolType(String symbolType) {
        this.symbolType = symbolType;
    }

    public String getSourceRange() {
        return sourceRange;
    }

    public void setSourceRange(String sourceRange) {
        this.sourceRange = sourceRange;
    }

    public String getDocstring() {
        return docstring;
    }

    public void setDocstring(String docstring) {
        this.docstring = docstring;
    }

    public String getSymbolSignature() {
        return symbolSignature;
    }

    public void setSymbolSignature(String symbolSignature) {
        this.symbolSignature = symbolSignature;
    }

    public String getNodeProperties() {
        return nodeProperties;
    }

    public void setNodeProperties(String nodeProperties) {
        this.nodeProperties = nodeProperties;
    }
}
