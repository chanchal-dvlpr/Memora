package com.contextengine.application.knowledge.engine;

import com.contextengine.application.scanner.ScanCandidate;
import com.contextengine.application.scanner.SourceSymbol;
import com.contextengine.application.scanner.dependency.ProjectDependency;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Execution context carrying completed scanner details and metrics.
 */
public class KnowledgeEngineContext {

    private final String projectId;
    private final String workspaceId;
    private final String scanId;
    private final String structuralHash;
    private final Instant scanTimestamp;
    private final Map<String, Object> scannerStatistics;
    private final KnowledgeEngineConfiguration configuration;

    private final Collection<ScanCandidate> candidates;
    private final Collection<SourceSymbol> symbols;
    private final Collection<ProjectDependency> dependencies;

    private final Collection<String> addedPaths;
    private final Collection<String> modifiedPaths;
    private final Collection<String> deletedPaths;
    private final boolean isIncremental;

    /**
     * Legacy constructor for backwards compatibility in tests.
     */
    public KnowledgeEngineContext(
        String projectId,
        String workspaceId,
        String scanId,
        String structuralHash,
        Instant scanTimestamp,
        Map<String, Object> scannerStatistics,
        KnowledgeEngineConfiguration configuration
    ) {
        this(projectId, workspaceId, scanId, structuralHash, scanTimestamp, scannerStatistics, configuration,
             Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
             Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false);
    }

    /**
     * Constructs a full KnowledgeEngineContext.
     */
    public KnowledgeEngineContext(
        String projectId,
        String workspaceId,
        String scanId,
        String structuralHash,
        Instant scanTimestamp,
        Map<String, Object> scannerStatistics,
        KnowledgeEngineConfiguration configuration,
        Collection<ScanCandidate> candidates,
        Collection<SourceSymbol> symbols,
        Collection<ProjectDependency> dependencies,
        Collection<String> addedPaths,
        Collection<String> modifiedPaths,
        Collection<String> deletedPaths,
        boolean isIncremental
    ) {
        this.projectId = Objects.requireNonNull(projectId, "ProjectId must not be null");
        this.workspaceId = Objects.requireNonNull(workspaceId, "WorkspaceId must not be null");
        this.scanId = Objects.requireNonNull(scanId, "ScanId must not be null");
        this.structuralHash = Objects.requireNonNull(structuralHash, "StructuralHash must not be null");
        this.scanTimestamp = Objects.requireNonNull(scanTimestamp, "ScanTimestamp must not be null");
        this.scannerStatistics = scannerStatistics != null 
                ? Collections.unmodifiableMap(new HashMap<>(scannerStatistics)) 
                : Collections.emptyMap();
        this.configuration = Objects.requireNonNull(configuration, "Configuration must not be null");
        this.candidates = candidates != null ? new ArrayList<>(candidates) : Collections.emptyList();
        this.symbols = symbols != null ? new ArrayList<>(symbols) : Collections.emptyList();
        this.dependencies = dependencies != null ? new ArrayList<>(dependencies) : Collections.emptyList();
        this.addedPaths = addedPaths != null ? new ArrayList<>(addedPaths) : Collections.emptyList();
        this.modifiedPaths = modifiedPaths != null ? new ArrayList<>(modifiedPaths) : Collections.emptyList();
        this.deletedPaths = deletedPaths != null ? new ArrayList<>(deletedPaths) : Collections.emptyList();
        this.isIncremental = isIncremental;
    }

    public String projectId() {
        return projectId;
    }

    public String workspaceId() {
        return workspaceId;
    }

    public String scanId() {
        return scanId;
    }

    public String structuralHash() {
        return structuralHash;
    }

    public Instant scanTimestamp() {
        return scanTimestamp;
    }

    public Map<String, Object> scannerStatistics() {
        return scannerStatistics;
    }

    public KnowledgeEngineConfiguration configuration() {
        return configuration;
    }

    public Collection<ScanCandidate> candidates() {
        return Collections.unmodifiableCollection(candidates);
    }

    public Collection<SourceSymbol> symbols() {
        return Collections.unmodifiableCollection(symbols);
    }

    public Collection<ProjectDependency> dependencies() {
        return Collections.unmodifiableCollection(dependencies);
    }

    public Collection<String> addedPaths() {
        return Collections.unmodifiableCollection(addedPaths);
    }

    public Collection<String> modifiedPaths() {
        return Collections.unmodifiableCollection(modifiedPaths);
    }

    public Collection<String> deletedPaths() {
        return Collections.unmodifiableCollection(deletedPaths);
    }

    public boolean isIncremental() {
        return isIncremental;
    }
}
