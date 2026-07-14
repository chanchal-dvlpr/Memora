package com.contextengine.application.knowledge.engine;

import com.contextengine.application.knowledge.graph.GraphValidator;
import com.contextengine.application.knowledge.graph.GraphValidationResult;
import com.contextengine.application.knowledge.graph.GraphUpdateEngine;
import com.contextengine.application.knowledge.graph.KnowledgeGraph;
import com.contextengine.application.knowledge.graph.KnowledgeGraphBuilder;

import java.time.Instant;
import java.util.Objects;

/**
 * Default implementation of KnowledgeEngine orchestration layer coordinating graph building and validation.
 */
public class KnowledgeEngineImpl implements KnowledgeEngine {

    private final GraphUpdateEngine updateEngine;
    private final KnowledgeGraphBuilder graphBuilder;
    private final GraphValidator graphValidator;

    public KnowledgeEngineImpl() {
        this.updateEngine = new GraphUpdateEngine();
        this.graphBuilder = new KnowledgeGraphBuilder(updateEngine);
        this.graphValidator = new GraphValidator();
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
                stats.setProcessingDurationMs(Instant.now().toEpochMilli() - start);
                return new KnowledgeEngineResult(
                    context.projectId(),
                    context.scanId(),
                    "WARNING",
                    stats,
                    Instant.now(),
                    graph
                );
            }
        }

        stats.setProcessingDurationMs(Instant.now().toEpochMilli() - start);
        return new KnowledgeEngineResult(
            context.projectId(),
            context.scanId(),
            "COMPLETED",
            stats,
            Instant.now(),
            graph
        );
    }
}
