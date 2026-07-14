package com.contextengine.application.knowledge.ranking;

import com.contextengine.application.knowledge.context.ContextFragment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of RelevanceRankingEngine orchestrating fragment scoring, deterministic ordering, validation, and incremental updates.
 */
public class RelevanceRankingEngineImpl implements RelevanceRankingEngine {

    private final IncrementalRankingEngine incrementalEngine = new IncrementalRankingEngine();
    private final List<RankingStrategy> strategies = List.of(
        new StructuralRankingStrategy(),
        new DependencyRankingStrategy(),
        new SymbolRankingStrategy(),
        new RecencyRankingStrategy()
    );

    @Override
    public RankingResult rank(RankingContext context) {
        Objects.requireNonNull(context, "context must not be null");
        long start = System.currentTimeMillis();
        RankingStatistics stats = new RankingStatistics();

        List<ContextFragment> sourceFragments = context.assemblyResult().fragments();
        List<RankedFragment> ranked = new ArrayList<>();

        // 1. Check for complete reuse if structural hash matches exactly
        if (context.isIncremental() && incrementalEngine.isUnchanged(context.assemblyResult().projectId(), context.structuralHash())) {
            RankingResult cachedResult = incrementalEngine.getCachedResult(context.assemblyResult().projectId());
            if (cachedResult != null) {
                return cachedResult;
            }
        }
        // 2. Incremental partial reuse check
        Map<String, RankedFragment> cacheMap = Collections.emptyMap();
        if (context.isIncremental()) {
            cacheMap = incrementalEngine.getCachedFragments(context.assemblyResult().projectId());
        }

        for (ContextFragment fragment : sourceFragments) {
            stats.incrementFragmentsProcessed(1);

            RankedFragment rf = null;
            // If it is incremental and fragment source path is not dirty, try to reuse cached score
            if (context.isIncremental() && !context.dirtyPaths().contains(fragment.sourcePath())) {
                rf = cacheMap.get(fragment.fragmentId());
            }

            if (rf != null) {
                rf = new RankedFragment(fragment, rf.score());
            } else {
                RankingScore score = calculateScore(fragment, context.configuration());
                rf = new RankedFragment(fragment, score);
            }

            ranked.add(rf);
            stats.incrementFragmentsRanked(1);
        }

        // Deterministic sort: sorts descending by score value, falls back lexicographically on fragment ID
        Collections.sort(ranked);

        // Limit results to maximumCandidates bounds
        List<RankedFragment> finalRanked = ranked;
        int maxCandidates = context.configuration().maximumCandidates();
        if (ranked.size() > maxCandidates) {
            finalRanked = ranked.subList(0, maxCandidates);
            stats.incrementSkippedFragments(ranked.size() - maxCandidates);
        }

        // 3. Validation execution
        long validateStart = System.currentTimeMillis();
        RankingValidator validator = new RankingValidator();
        RankingValidationResult validationResult = validator.validate(finalRanked, context.configuration());
        stats.setValidationDuration(System.currentTimeMillis() - validateStart);

        if (!validationResult.isValid()) {
            stats.incrementWarnings(validationResult.errors().size());
        }

        // 4. Compute extended stats
        double totalScore = 0.0;
        double highest = Double.NEGATIVE_INFINITY;
        double lowest = Double.POSITIVE_INFINITY;

        for (RankedFragment rf : finalRanked) {
            double val = rf.score().value();
            totalScore += val;
            if (val > highest) {
                highest = val;
            }
            if (val < lowest) {
                lowest = val;
            }
        }

        double average = finalRanked.isEmpty() ? 0.0 : totalScore / finalRanked.size();
        stats.setAverageScore(average);
        stats.setHighestScore(finalRanked.isEmpty() ? 0.0 : highest);
        stats.setLowestScore(finalRanked.isEmpty() ? 0.0 : lowest);

        stats.setRankingDuration(System.currentTimeMillis() - start);

        RankingResult result = new RankingResult(
            context.assemblyResult().projectId(),
            finalRanked,
            stats,
            Instant.now()
        );

        // Cache the computed state for future incremental runs (caching the full list before truncation)
        incrementalEngine.cacheResult(context.assemblyResult().projectId(), context.structuralHash(), ranked, result);

        return result;
    }

    private RankingScore calculateScore(ContextFragment fragment, RankingConfiguration config) {
        double value = 0.0;
        List<RankingReason> reasons = new ArrayList<>();
        for (RankingStrategy strategy : strategies) {
            RankingReason reason = strategy.evaluate(fragment, config);
            if (reason.contribution() > 0.0) {
                value += reason.contribution();
                reasons.add(reason);
            }
        }
        return new RankingScore(value, reasons);
    }
}
