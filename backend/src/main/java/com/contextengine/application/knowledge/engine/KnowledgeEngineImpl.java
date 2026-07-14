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
import com.contextengine.application.knowledge.snapshot.*;
import com.contextengine.domain.entity.ContextSnapshot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of KnowledgeEngine orchestration layer coordinating graph building, validation, context assembly, relevance ranking, and snapshot comparison.
 */
public class KnowledgeEngineImpl implements KnowledgeEngine {

    private final GraphUpdateEngine updateEngine;
    private final KnowledgeGraphBuilder graphBuilder;
    private final GraphValidator graphValidator;
    private final ContextAssemblyEngine assemblyEngine;
    private final RelevanceRankingEngine rankingEngine;
    private final com.contextengine.application.knowledge.budget.ContextBudgetManager budgetManager;
    private final SnapshotComparisonPipeline comparisonPipeline;
    private final com.contextengine.application.knowledge.search.SearchPipeline searchPipeline;

    private final java.util.Map<String, ContextSnapshot> latestSnapshots = new ConcurrentHashMap<>();

    public KnowledgeEngineImpl() {
        this(new ContextAssemblyEngineImpl(), new RelevanceRankingEngineImpl(), new com.contextengine.application.knowledge.budget.ContextBudgetManagerImpl(), new SnapshotComparisonEngineImpl(), new com.contextengine.application.knowledge.search.SearchEngineImpl());
    }

    public KnowledgeEngineImpl(ContextAssemblyEngine assemblyEngine) {
        this(assemblyEngine, new RelevanceRankingEngineImpl(), new com.contextengine.application.knowledge.budget.ContextBudgetManagerImpl(), new SnapshotComparisonEngineImpl(), new com.contextengine.application.knowledge.search.SearchEngineImpl());
    }

    public KnowledgeEngineImpl(ContextAssemblyEngine assemblyEngine, RelevanceRankingEngine rankingEngine) {
        this(assemblyEngine, rankingEngine, new com.contextengine.application.knowledge.budget.ContextBudgetManagerImpl(), new SnapshotComparisonEngineImpl(), new com.contextengine.application.knowledge.search.SearchEngineImpl());
    }

    public KnowledgeEngineImpl(
        ContextAssemblyEngine assemblyEngine,
        RelevanceRankingEngine rankingEngine,
        com.contextengine.application.knowledge.budget.ContextBudgetManager budgetManager
    ) {
        this(assemblyEngine, rankingEngine, budgetManager, new SnapshotComparisonEngineImpl(), new com.contextengine.application.knowledge.search.SearchEngineImpl());
    }

    public KnowledgeEngineImpl(
        ContextAssemblyEngine assemblyEngine,
        RelevanceRankingEngine rankingEngine,
        com.contextengine.application.knowledge.budget.ContextBudgetManager budgetManager,
        SnapshotComparisonEngine comparisonEngine
    ) {
        this(assemblyEngine, rankingEngine, budgetManager, comparisonEngine, new com.contextengine.application.knowledge.search.SearchEngineImpl());
    }

    public KnowledgeEngineImpl(
        ContextAssemblyEngine assemblyEngine,
        RelevanceRankingEngine rankingEngine,
        com.contextengine.application.knowledge.budget.ContextBudgetManager budgetManager,
        SnapshotComparisonEngine comparisonEngine,
        com.contextengine.application.knowledge.search.SearchEngine searchEngine
    ) {
        this.updateEngine = new GraphUpdateEngine();
        this.graphBuilder = new KnowledgeGraphBuilder(updateEngine);
        this.graphValidator = new GraphValidator();
        this.assemblyEngine = Objects.requireNonNull(assemblyEngine, "assemblyEngine must not be null");
        this.rankingEngine = Objects.requireNonNull(rankingEngine, "rankingEngine must not be null");
        this.budgetManager = Objects.requireNonNull(budgetManager, "budgetManager must not be null");
        this.comparisonPipeline = new SnapshotComparisonPipeline(Objects.requireNonNull(comparisonEngine, "comparisonEngine must not be null"));
        this.searchPipeline = new com.contextengine.application.knowledge.search.SearchPipeline(Objects.requireNonNull(searchEngine, "searchEngine must not be null"));
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
                null,
                null,
                null,
                null,
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
                    null,
                    null,
                    null,
                    null,
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
                    graph,
                    null,
                    null,
                    null,
                    null
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
                List<String> dirtyPaths = new ArrayList<>();
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

                com.contextengine.application.knowledge.budget.BudgetConfiguration budgetConfig = new com.contextengine.application.knowledge.budget.BudgetConfiguration();
                com.contextengine.application.knowledge.budget.BudgetContext budgetContext = new com.contextengine.application.knowledge.budget.BudgetContext(
                    rankingResult,
                    budgetConfig,
                    context.structuralHash(),
                    context.isIncremental(),
                    dirtyPaths
                );
                com.contextengine.application.knowledge.budget.BudgetResult budgetResult = budgetManager.budget(budgetContext);

                // Run snapshot comparison
                ContextSnapshot currentSnapshot = toSnapshot(budgetResult);
                ContextSnapshot previousSnapshot = latestSnapshots.get(context.projectId());
                SnapshotComparisonResult comparisonResult = comparisonPipeline.execute(
                    previousSnapshot, currentSnapshot, new SnapshotComparisonConfiguration(),
                    context.structuralHash(), context.isIncremental()
                );
                latestSnapshots.put(context.projectId(), currentSnapshot);

                // Run search & retrieval
                com.contextengine.application.knowledge.search.SearchResult searchResult = searchPipeline.execute(
                    graph,
                    new com.contextengine.application.knowledge.search.SearchQuery("", new java.util.HashMap<>()),
                    new com.contextengine.application.knowledge.search.SearchConfiguration(),
                    context.structuralHash(),
                    context.isIncremental()
                );

                stats.setProcessingDurationMs(Instant.now().toEpochMilli() - start);
                return new KnowledgeEngineResult(
                    context.projectId(),
                    context.scanId(),
                    "WARNING",
                    stats,
                    Instant.now(),
                    graph,
                    assemblyResult,
                    rankingResult,
                    budgetResult,
                    comparisonResult,
                    searchResult
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
        List<String> dirtyPaths = new ArrayList<>();
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

        com.contextengine.application.knowledge.budget.BudgetConfiguration budgetConfig = new com.contextengine.application.knowledge.budget.BudgetConfiguration();
        com.contextengine.application.knowledge.budget.BudgetContext budgetContext = new com.contextengine.application.knowledge.budget.BudgetContext(
            rankingResult,
            budgetConfig,
            context.structuralHash(),
            context.isIncremental(),
            dirtyPaths
        );
        com.contextengine.application.knowledge.budget.BudgetResult budgetResult = budgetManager.budget(budgetContext);

        // Run snapshot comparison
        ContextSnapshot currentSnapshot = toSnapshot(budgetResult);
        ContextSnapshot previousSnapshot = latestSnapshots.get(context.projectId());
        SnapshotComparisonResult comparisonResult = comparisonPipeline.execute(
            previousSnapshot, currentSnapshot, new SnapshotComparisonConfiguration(),
            context.structuralHash(), context.isIncremental()
        );
        latestSnapshots.put(context.projectId(), currentSnapshot);

        // Run search & retrieval
        com.contextengine.application.knowledge.search.SearchResult searchResult = searchPipeline.execute(
            graph,
            new com.contextengine.application.knowledge.search.SearchQuery("", new java.util.HashMap<>()),
            new com.contextengine.application.knowledge.search.SearchConfiguration(),
            context.structuralHash(),
            context.isIncremental()
        );

        stats.setProcessingDurationMs(Instant.now().toEpochMilli() - start);
        return new KnowledgeEngineResult(
            context.projectId(),
            context.scanId(),
            "COMPLETED",
            stats,
            Instant.now(),
            graph,
            assemblyResult,
            rankingResult,
            budgetResult,
            comparisonResult,
            searchResult
        );
    }

    private ContextSnapshot toSnapshot(com.contextengine.application.knowledge.budget.BudgetResult budgetResult) {
        Objects.requireNonNull(budgetResult, "budgetResult must not be null");
        com.contextengine.domain.valueobject.SnapshotId snapshotId = com.contextengine.domain.valueobject.SnapshotId.generate();

        UUID projUuid;
        try {
            projUuid = UUID.fromString(budgetResult.projectId());
            if (projUuid.version() != 4) {
                projUuid = UUID.randomUUID();
            }
        } catch (IllegalArgumentException e) {
            projUuid = UUID.randomUUID();
        }
        com.contextengine.domain.valueobject.ProjectId projectId = new com.contextengine.domain.valueobject.ProjectId(projUuid);

        com.contextengine.domain.valueobject.Version version = new com.contextengine.domain.valueobject.Version(1);
        com.contextengine.domain.valueobject.Timestamp createdAt = com.contextengine.domain.valueobject.Timestamp.now();

        List<com.contextengine.domain.valueobject.EngineeringEvidence> evidences = new ArrayList<>();
        int totalFileCount = 0;
        int tokenFootprint = 0;
        List<String> primaryEntities = new ArrayList<>();

        for (com.contextengine.application.knowledge.budget.BudgetedFragment bf : budgetResult.budgetedFragments()) {
            if (bf.decision() == com.contextengine.application.knowledge.budget.BudgetDecision.INCLUDED ||
                bf.decision() == com.contextengine.application.knowledge.budget.BudgetDecision.RESERVED) {
                
                com.contextengine.application.knowledge.context.ContextFragment fragment = bf.rankedFragment().fragment();
                if (fragment.fragmentType() == com.contextengine.application.knowledge.context.ContextFragmentType.FILE) {
                    totalFileCount++;
                }

                tokenFootprint += fragment.estimatedTokens();
                primaryEntities.add("urn:ce:fragment:" + fragment.fragmentId());

                String pathStr = fragment.sourcePath();
                if (pathStr != null && !pathStr.isBlank()) {
                    int startLine = 1;
                    int endLine = 1;
                    
                    Object sl = fragment.metadata().get("startLine");
                    if (sl instanceof Number) {
                        startLine = ((Number) sl).intValue();
                    }
                    Object el = fragment.metadata().get("endLine");
                    if (el instanceof Number) {
                        endLine = ((Number) el).intValue();
                    }
                    
                    if (startLine < 1) startLine = 1;
                    if (endLine < startLine) endLine = startLine;

                    String hashVal = (String) fragment.metadata().get("contentHash");
                    if (hashVal == null || hashVal.isBlank() || hashVal.length() != 64) {
                        hashVal = sha256(fragment.content());
                    }
                    com.contextengine.domain.valueobject.Hash contentHash = new com.contextengine.domain.valueobject.Hash(hashVal);

                    try {
                        evidences.add(new com.contextengine.domain.valueobject.EngineeringEvidence(
                            new com.contextengine.domain.valueobject.Path(pathStr),
                            startLine,
                            endLine,
                            contentHash
                        ));
                    } catch (Exception ignored) {}
                }
            }
        }

        com.contextengine.domain.valueobject.ContextSummary summary = new com.contextengine.domain.valueobject.ContextSummary(
            totalFileCount, tokenFootprint, primaryEntities
        );

        return new ContextSnapshot(snapshotId, projectId, version, createdAt, summary, evidences);
    }

    private String sha256(String text) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "a".repeat(64);
        }
    }
}
