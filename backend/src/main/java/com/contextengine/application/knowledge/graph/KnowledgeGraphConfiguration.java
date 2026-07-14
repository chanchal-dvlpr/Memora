package com.contextengine.application.knowledge.graph;

/**
 * Settings configurations for validation and structural rules of the KnowledgeGraph.
 */
public class KnowledgeGraphConfiguration {

    private final boolean allowSelfLoops;
    private final boolean validateNodeTypes;

    /**
     * Constructs settings with default boundaries (no self loops allowed, nodes validated).
     */
    public KnowledgeGraphConfiguration() {
        this(false, true);
    }

    /**
     * Constructs settings with customized parameters.
     *
     * @param allowSelfLoops permit node relations mapping to themselves
     * @param validateNodeTypes validate matching structures
     */
    public KnowledgeGraphConfiguration(boolean allowSelfLoops, boolean validateNodeTypes) {
        this.allowSelfLoops = allowSelfLoops;
        this.validateNodeTypes = validateNodeTypes;
    }

    public boolean allowSelfLoops() {
        return allowSelfLoops;
    }

    public boolean validateNodeTypes() {
        return validateNodeTypes;
    }
}
