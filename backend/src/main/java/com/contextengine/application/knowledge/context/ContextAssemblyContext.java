package com.contextengine.application.knowledge.context;

import com.contextengine.application.knowledge.graph.KnowledgeGraph;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Assembly context combining the source KnowledgeGraph and current configuration.
 */
public class ContextAssemblyContext {
    private final KnowledgeGraph graph;
    private final ContextAssemblyConfiguration configuration;
    private final Collection<String> addedPaths;
    private final Collection<String> modifiedPaths;
    private final Collection<String> deletedPaths;
    private final boolean isIncremental;
    private final String structuralHash;

    /**
     * Backward-compatible constructor for non-incremental runs.
     */
    public ContextAssemblyContext(KnowledgeGraph graph, ContextAssemblyConfiguration configuration) {
        this(graph, configuration, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, "");
    }

    /**
     * Context constructor carrying incremental changes.
     */
    public ContextAssemblyContext(
        KnowledgeGraph graph,
        ContextAssemblyConfiguration configuration,
        Collection<String> addedPaths,
        Collection<String> modifiedPaths,
        Collection<String> deletedPaths,
        boolean isIncremental,
        String structuralHash
    ) {
        this.graph = Objects.requireNonNull(graph, "Graph must not be null");
        this.configuration = Objects.requireNonNull(configuration, "Configuration must not be null");
        this.addedPaths = addedPaths != null ? addedPaths : Collections.emptyList();
        this.modifiedPaths = modifiedPaths != null ? modifiedPaths : Collections.emptyList();
        this.deletedPaths = deletedPaths != null ? deletedPaths : Collections.emptyList();
        this.isIncremental = isIncremental;
        this.structuralHash = structuralHash != null ? structuralHash : "";
    }

    public KnowledgeGraph graph() {
        return graph;
    }

    public ContextAssemblyConfiguration configuration() {
        return configuration;
    }

    public Collection<String> addedPaths() {
        return addedPaths;
    }

    public Collection<String> modifiedPaths() {
        return modifiedPaths;
    }

    public Collection<String> deletedPaths() {
        return deletedPaths;
    }

    public boolean isIncremental() {
        return isIncremental;
    }

    public String structuralHash() {
        return structuralHash;
    }
}
