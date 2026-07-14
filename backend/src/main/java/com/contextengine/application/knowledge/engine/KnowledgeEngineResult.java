package com.contextengine.application.knowledge.engine;

import com.contextengine.application.knowledge.context.ContextAssemblyResult;
import com.contextengine.application.knowledge.graph.KnowledgeGraph;
import com.contextengine.application.knowledge.ranking.RankingResult;

import java.time.Instant;
import java.util.Objects;

/**
 * Result envelope of a Knowledge Engine execution phase.
 */
public class KnowledgeEngineResult {

    private final String projectId;
    private final String scanId;
    private final String processingStatus; // "COMPLETED", "FAILED", "WARNING"
    private final KnowledgeEngineStatistics statistics;
    private final Instant generatedTimestamp;
    private final KnowledgeGraph graph;
    private final ContextAssemblyResult contextAssemblyResult;
    private final RankingResult rankingResult;

    /**
     * Legacy constructor for backwards compatibility.
     */
    public KnowledgeEngineResult(
        String projectId,
        String scanId,
        String processingStatus,
        KnowledgeEngineStatistics statistics,
        Instant generatedTimestamp
    ) {
        this(projectId, scanId, processingStatus, statistics, generatedTimestamp, null, null, null);
    }

    /**
     * Constructs a KnowledgeEngineResult carrying the built graph.
     */
    public KnowledgeEngineResult(
        String projectId,
        String scanId,
        String processingStatus,
        KnowledgeEngineStatistics statistics,
        Instant generatedTimestamp,
        KnowledgeGraph graph
    ) {
        this(projectId, scanId, processingStatus, statistics, generatedTimestamp, graph, null, null);
    }

    /**
     * Constructs a full KnowledgeEngineResult carrying the built graph and context assembly result.
     */
    public KnowledgeEngineResult(
        String projectId,
        String scanId,
        String processingStatus,
        KnowledgeEngineStatistics statistics,
        Instant generatedTimestamp,
        KnowledgeGraph graph,
        ContextAssemblyResult contextAssemblyResult
    ) {
        this(projectId, scanId, processingStatus, statistics, generatedTimestamp, graph, contextAssemblyResult, null);
    }

    /**
     * Constructs a full KnowledgeEngineResult carrying the built graph, context assembly, and ranking results.
     */
    public KnowledgeEngineResult(
        String projectId,
        String scanId,
        String processingStatus,
        KnowledgeEngineStatistics statistics,
        Instant generatedTimestamp,
        KnowledgeGraph graph,
        ContextAssemblyResult contextAssemblyResult,
        RankingResult rankingResult
    ) {
        this.projectId = Objects.requireNonNull(projectId, "ProjectId must not be null");
        this.scanId = Objects.requireNonNull(scanId, "ScanId must not be null");
        this.processingStatus = Objects.requireNonNull(processingStatus, "ProcessingStatus must not be null");
        this.statistics = Objects.requireNonNull(statistics, "Statistics must not be null");
        this.generatedTimestamp = Objects.requireNonNull(generatedTimestamp, "GeneratedTimestamp must not be null");
        this.graph = graph;
        this.contextAssemblyResult = contextAssemblyResult;
        this.rankingResult = rankingResult;
    }

    public String projectId() {
        return projectId;
    }

    public String scanId() {
        return scanId;
    }

    public String processingStatus() {
        return processingStatus;
    }

    public KnowledgeEngineStatistics statistics() {
        return statistics;
    }

    public Instant generatedTimestamp() {
        return generatedTimestamp;
    }

    public KnowledgeGraph graph() {
        return graph;
    }

    public ContextAssemblyResult contextAssemblyResult() {
        return contextAssemblyResult;
    }

    public RankingResult rankingResult() {
        return rankingResult;
    }
}
