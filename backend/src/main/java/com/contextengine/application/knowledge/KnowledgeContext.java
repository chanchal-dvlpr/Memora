package com.contextengine.application.knowledge;

import com.contextengine.domain.valueobject.NodeId;
import com.contextengine.domain.valueobject.ProjectId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Tracks the state and context of a Knowledge Graph construction session.
 * Holds project information and resolves entity URNs to unique Node IDs to prevent duplication.
 * <p>
 * Bounded Context: Knowledge Graph
 * Related Subsystem: Knowledge Graph Engine (KG-ENG)
 * Reference: Functional Requirement FR-016 (Knowledge Graph Engine) Section 7 (URN resolution)
 * </p>
 */
public class KnowledgeContext {
    private final ProjectId projectId;
    private final String scanMode;
    private final Map<String, NodeId> urnToNodeIdMap;
    private final Map<String, Object> attributes;

    /**
     * Constructs a KnowledgeContext.
     *
     * @param projectId the parent project ID
     * @param scanMode the scan modality (FULL or INCREMENTAL)
     */
    public KnowledgeContext(ProjectId projectId, String scanMode) {
        this.projectId = Objects.requireNonNull(projectId, "ProjectId must not be null");
        this.scanMode = Objects.requireNonNull(scanMode, "ScanMode must not be null");
        this.urnToNodeIdMap = new HashMap<>();
        this.attributes = new HashMap<>();
    }

    /**
     * Returns the associated project ID.
     *
     * @return the ProjectId
     */
    public ProjectId projectId() {
        return projectId;
    }

    /**
     * Returns the scan modality.
     *
     * @return scan mode string
     */
    public String scanMode() {
        return scanMode;
    }

    /**
     * Resolves a deterministic URN to a unique NodeId.
     * If the URN is encountered for the first time, a new NodeId is generated.
     *
     * @param urn the deterministic URN of the node
     * @return the resolved NodeId
     */
    public NodeId resolveNodeId(String urn) {
        Objects.requireNonNull(urn, "URN must not be null");
        return urnToNodeIdMap.computeIfAbsent(urn, k -> NodeId.generate());
    }

    /**
     * Registers a custom attribute value in the context.
     *
     * @param key the attribute key
     * @param value the attribute value
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * Retrieves a custom attribute value from the context.
     *
     * @param key the attribute key
     * @return the attribute value, or null if not found
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }
}
