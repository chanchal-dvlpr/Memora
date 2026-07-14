package com.contextengine.application.knowledge.context;

/**
 * Configuration options controlling the depth and boundaries of context assembly.
 */
public class ContextAssemblyConfiguration {
    private final boolean includeDependencies;
    private final boolean includeDirectories;
    private final boolean includeSymbols;
    private final boolean includeWorkspaceMetadata;
    private final int maximumAssemblyDepth;

    /**
     * Constructs a default configuration.
     */
    public ContextAssemblyConfiguration() {
        this(true, true, true, true, 10);
    }

    /**
     * Constructs a custom configuration.
     */
    public ContextAssemblyConfiguration(
        boolean includeDependencies,
        boolean includeDirectories,
        boolean includeSymbols,
        boolean includeWorkspaceMetadata,
        int maximumAssemblyDepth
    ) {
        this.includeDependencies = includeDependencies;
        this.includeDirectories = includeDirectories;
        this.includeSymbols = includeSymbols;
        this.includeWorkspaceMetadata = includeWorkspaceMetadata;
        this.maximumAssemblyDepth = maximumAssemblyDepth;
    }

    public boolean includeDependencies() {
        return includeDependencies;
    }

    public boolean includeDirectories() {
        return includeDirectories;
    }

    public boolean includeSymbols() {
        return includeSymbols;
    }

    public boolean includeWorkspaceMetadata() {
        return includeWorkspaceMetadata;
    }

    public int maximumAssemblyDepth() {
        return maximumAssemblyDepth;
    }
}
