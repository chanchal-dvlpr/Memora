package com.contextengine.application.knowledge.engine;

import java.util.Objects;

/**
 * Settings configuration for semantic expansions, depth, and strictness rules.
 */
public class KnowledgeEngineConfiguration {

    private final int maxGraphDepth;
    private final boolean enableDependencyExpansion;
    private final boolean enableSymbolRelationships;
    private final boolean enableSemanticEnrichment;
    private final String validationMode; // "STRICT" or "LAX"

    /**
     * Constructs configurations with default parameters.
     */
    public KnowledgeEngineConfiguration() {
        this(5, true, true, true, "STRICT");
    }

    /**
     * Constructs a customized configuration.
     *
     * @param maxGraphDepth limit of graph lookups depth
     * @param enableDependencyExpansion enable analysis of external manifest nodes
     * @param enableSymbolRelationships map class call hierarchies
     * @param enableSemanticEnrichment run LLM/heuristics tagging
     * @param validationMode schema check policy
     */
    public KnowledgeEngineConfiguration(
        int maxGraphDepth,
        boolean enableDependencyExpansion,
        boolean enableSymbolRelationships,
        boolean enableSemanticEnrichment,
        String validationMode
    ) {
        this.maxGraphDepth = maxGraphDepth;
        this.enableDependencyExpansion = enableDependencyExpansion;
        this.enableSymbolRelationships = enableSymbolRelationships;
        this.enableSemanticEnrichment = enableSemanticEnrichment;
        this.validationMode = Objects.requireNonNull(validationMode, "ValidationMode must not be null");
    }

    public int maxGraphDepth() {
        return maxGraphDepth;
    }

    public boolean enableDependencyExpansion() {
        return enableDependencyExpansion;
    }

    public boolean enableSymbolRelationships() {
        return enableSymbolRelationships;
    }

    public boolean enableSemanticEnrichment() {
        return enableSemanticEnrichment;
    }

    public String validationMode() {
        return validationMode;
    }
}
