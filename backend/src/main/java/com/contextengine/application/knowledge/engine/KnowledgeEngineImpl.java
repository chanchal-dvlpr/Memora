package com.contextengine.application.knowledge.engine;

import com.contextengine.application.knowledge.context.ContextAssemblyConfiguration;
import com.contextengine.application.knowledge.context.ContextAssemblyContext;
import com.contextengine.application.knowledge.context.ContextAssemblyEngine;
import com.contextengine.application.knowledge.context.ContextAssemblyEngineImpl;
import com.contextengine.application.knowledge.context.ContextAssemblyResult;
import com.contextengine.application.knowledge.graph.GraphValidator;
import com.contextengine.application.knowledge.graph.GraphValidationResult;
import com.contextengine.application.knowledge.graph.GraphUpdateEngine;
import com.contextengine.application.knowledge.graph.KnowledgeGraph;
import com.contextengine.application.knowledge.graph.KnowledgeGraphBuilder;
import com.contextengine.application.knowledge.ranking.RankingConfiguration;
import com.contextengine.application.knowledge.ranking.RankingContext;
import com.contextengine.application.knowledge.ranking.RankingResult;
import com.contextengine.application.knowledge.ranking.RelevanceRankingEngine;
import com.contextengine.application.knowledge.ranking.RelevanceRankingEngineImpl;

import java.time.Instant;
import java.util.Objects;

/**
 * Default implementation of KnowledgeEngine orchestration layer coordinating graph building, validation, context assembly, and relevance ranking.
 */
public class KnowledgeEngineImpl implements KnowledgeEngine {

    private final GraphUpdateEngine updateEngine;
    private final KnowledgeGraphBuilder graphBuilder;
    private final GraphValidator graphValidator;
    private final ContextAssemblyEngine assemblyEngine;
    private final RelevanceRankingEngine rankingEngine;

    public KnowledgeEngineImpl() {
        this(new ContextAssemblyEngineImpl(), new RelevanceRankingEngineImpl());
    }

    public KnowledgeEngineImpl(ContextAssemblyEngine assemblyEngine) {
        this(assemblyEngine, new RelevanceRankingEngineImpl());
    }

    public KnowledgeEngineImpl(ContextAssemblyEngine assemblyEngine, RelevanceRankingEngine rankingEngine) {
        this.updateEngine = new GraphUpdateEngine();
        this.graphBuilder = new KnowledgeGraphBuilder(updateEngine);
        this.graphValidator = new GraphValidator();
        this.assemblyEngine = Objects.requireNonNull(assemblyEngine, "assemblyEngine must not be null");
        this.rankingEngine = Objects.requireNonNull(rankingEngine, "rankingEngine must not be null");
    }

    @Override
    public KnowledgeEngineResult process(KnowledgeEngineContext context) {
        Objects.requireNonNull(context, "Context must not be null");
        long start = Instant.now().toEpochMilli();

        KnowledgeEngineStatistics stats = new KnowledgeEngineStatistics();

        // 1. Basic validation of scanner inputs
        if (context.projectId().isEmpty() || context.scanId().isEmpty()) {
            stats.incrementWarnings(1);
            stats.setProcessingDurationMs(Instant.now().toEpochMilli() - start);
            return new KnowledgeEngineResult(
                context.projectId(),
                context.scanId(),
                "FAILED",
                stats,
                Instant.now(),
                null
            );
        }

        // 2. Structural hash validation rules
        if (context.structuralHash().isEmpty()) {
            stats.incrementWarnings(1);
            if ("STRICT".equalsIgnoreCase(context.configuration().validationMode())) {
                stats.setProcessingDurationMs(Instant.now().toEpochMilli() - start);
                return new KnowledgeEngineResult(
                    context.projectId(),
                    context.scanId(),
                    "FAILED",
                    stats,
                    Instant.now(),
                    null
                );
            }
        }

        // 3. Build the Knowledge Graph
        long buildStart = Instant.now().toEpochMilli();
        KnowledgeGraph graph = graphBuilder.build(context);
        long buildEnd = Instant.now().toEpochMilli();
        long buildDuration = buildEnd - buildStart;
        graph.statistics().setBuildDurationMs(buildDuration);

        // 4. Validate the Knowledge Graph
        long validateStart = Instant.now().toEpochMilli();
        GraphValidationResult validationResult = graphValidator.validate(graph);
        long validateEnd = Instant.now().toEpochMilli();
        long validationDuration = validateEnd - validateStart;
        graph.statistics().setValidationDurationMs(validationDuration);

        // Map counts to final Statistics
        long nodesProcessed = graph.nodes().size();
        long relationshipsProcessed = graph.relationships().size();

        if (relationshipsProcessed > 0) {
            relationshipsProcessed--;
        }

        // Fallback to legacy scanner stats if graph is empty (contains only Project and Workspace nodes)
        if (nodesProcessed <= 2) {
            long filesCountVal = 0;
            long symbolsCountVal = 0;
            Object filesCount = context.scannerStatistics().get("filesCount");
            if (filesCount instanceof Number) {
                filesCountVal = ((Number) filesCount).longValue();
            }
            Object symbolsCount = context.scannerStatistics().get("symbolsCount");
            if (symbolsCount instanceof Number) {
                symbolsCountVal = ((Number) symbolsCount).longValue();
            }
            if (filesCountVal > 0 || symbolsCountVal > 0) {
                nodesProcessed = filesCountVal + symbolsCountVal;
            }
        }

        stats.incrementNodesProcessed(nodesProcessed);
        stats.incrementRelationshipsProcessed(relationshipsProcessed);

        if (!validationResult.isValid()) {
            long errorCount = validationResult.errors().size();
            graph.statistics().setWarningCount(errorCount);
            stats.incrementWarnings(errorCount);

            if ("STRICT".equalsIgnoreCase(context.configuration().validationMode())) {
                stats.setProcessingDurationMs(Instant.now().toEpochMilli() - start);
                return new KnowledgeEngineResult(
                    context.projectId(),
                    context.scanId(),
                    "FAILED",
                    stats,
                    Instant.now(),
                    graph
                );
            } else {
                // Assembly in Lax mode even on warnings/errors
                ContextAssemblyConfiguration assemblyConfig = new ContextAssemblyConfiguration(
                    context.configuration().enableDependencyExpansion(),
                    true,
                    context.configuration().enableSymbolRelationships(),
                    context.configuration().enableSemanticEnrichment(),
                    context.configuration().maxGraphDepth()
                );
                ContextAssemblyContext assemblyContext = new ContextAssemblyContext(
                    graph,
                    assemblyConfig,
                    context.addedPaths(),
                    context.modifiedPaths(),
                    context.deletedPaths(),
                    context.isIncremental(),
                    context.structuralHash()
                );
                ContextAssemblyResult assemblyResult = assemblyEngine.assemble(assemblyContext);

                // Run relevance ranking in lax warning flow
                RankingConfiguration rankingConfig = new RankingConfiguration(
                    true,
                    true,
                    context.configuration().enableDependencyExpansion(),
                    context.configuration().enableSymbolRelationships(),
                    1000
                );
                java.util.List<String> dirtyPaths = new java.util.ArrayList<>();
                dirtyPaths.addAll(context.addedPaths());
                dirtyPaths.addAll(context.modifiedPaths());
                dirtyPaths.addAll(context.deletedPaths());

                RankingContext rankingContext = new RankingContext(
                    assemblyResult,
                    rankingConfig,
                    context.structuralHash(),
                    context.isIncremental(),
                    dirtyPaths
                );
                RankingResult rankingResult = rankingEngine.rank(rankingContext);

                stats.setProcessingDurationMs(Instant.now().toEpochMilli() - start);
                return new KnowledgeEngineResult(
                    context.projectId(),
                    context.scanId(),
                    "WARNING",
                    stats,
                    Instant.now(),
                    graph,
                    assemblyResult,
                    rankingResult
                );
            }
        }

        // Assemble Context Fragments
        ContextAssemblyConfiguration assemblyConfig = new ContextAssemblyConfiguration(
            context.configuration().enableDependencyExpansion(),
            true,
            context.configuration().enableSymbolRelationships(),
            context.configuration().enableSemanticEnrichment(),
            context.configuration().maxGraphDepth()
        );
        ContextAssemblyContext assemblyContext = new ContextAssemblyContext(
            graph,
            assemblyConfig,
            context.addedPaths(),
            context.modifiedPaths(),
            context.deletedPaths(),
            context.isIncremental(),
            context.structuralHash()
        );
        ContextAssemblyResult assemblyResult = assemblyEngine.assemble(assemblyContext);

        // Run relevance ranking
        RankingConfiguration rankingConfig = new RankingConfiguration(
            true,
            true,
            context.configuration().enableDependencyExpansion(),
            context.configuration().enableSymbolRelationships(),
            1000
        );
        java.util.List<String> dirtyPaths = new java.util.ArrayList<>();
        dirtyPaths.addAll(context.addedPaths());
        dirtyPaths.addAll(context.modifiedPaths());
        dirtyPaths.addAll(context.deletedPaths());

        RankingContext rankingContext = new RankingContext(
            assemblyResult,
            rankingConfig,
            context.structuralHash(),
            context.isIncremental(),
            dirtyPaths
        );
        RankingResult rankingResult = rankingEngine.rank(rankingContext);

        stats.setProcessingDurationMs(Instant.now().toEpochMilli() - start);
        return new KnowledgeEngineResult(
            context.projectId(),
            context.scanId(),
            "COMPLETED",
            stats,
            Instant.now(),
            graph,
            assemblyResult,
            rankingResult
        );
    }
}
