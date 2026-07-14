package com.contextengine.application.knowledge.ranking;

import com.contextengine.application.knowledge.context.ContextAssemblyResult;
import java.util.Objects;

/**
 * Execution context carrying assembled fragments and ranking rules.
 */
public class RankingContext {
    private final ContextAssemblyResult assemblyResult;
    private final RankingConfiguration configuration;
    private final String structuralHash;
    private final boolean isIncremental;
    private final java.util.Collection<String> dirtyPaths;

    public RankingContext(ContextAssemblyResult assemblyResult, RankingConfiguration configuration) {
        this(assemblyResult, configuration, "", false, java.util.Collections.emptyList());
    }

    public RankingContext(
        ContextAssemblyResult assemblyResult,
        RankingConfiguration configuration,
        String structuralHash,
        boolean isIncremental,
        java.util.Collection<String> dirtyPaths
    ) {
        this.assemblyResult = Objects.requireNonNull(assemblyResult, "assemblyResult must not be null");
        this.configuration = Objects.requireNonNull(configuration, "configuration must not be null");
        this.structuralHash = structuralHash != null ? structuralHash : "";
        this.isIncremental = isIncremental;
        this.dirtyPaths = dirtyPaths != null ? java.util.List.copyOf(dirtyPaths) : java.util.Collections.emptyList();
    }

    public ContextAssemblyResult assemblyResult() {
        return assemblyResult;
    }

    public RankingConfiguration configuration() {
        return configuration;
    }

    public String structuralHash() {
        return structuralHash;
    }

    public boolean isIncremental() {
        return isIncremental;
    }

    public java.util.Collection<String> dirtyPaths() {
        return dirtyPaths;
    }
}
