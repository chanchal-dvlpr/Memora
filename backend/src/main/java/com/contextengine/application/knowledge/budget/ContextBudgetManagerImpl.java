package com.contextengine.application.knowledge.budget;

import com.contextengine.application.knowledge.ranking.RankedFragment;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Strategy-driven implementation of ContextBudgetManager integrating validators and caching.
 */
public class ContextBudgetManagerImpl implements ContextBudgetManager {

    private final List<BudgetAlgorithm> strategies = List.of(
        new ReservationBudgetStrategy(),
        new FragmentCountBudgetStrategy(),
        new CharacterBudgetStrategy(),
        new TokenBudgetStrategy()
    );

    private final IncrementalBudgetManager incrementalManager = new IncrementalBudgetManager();

    @Override
    public BudgetResult budget(BudgetContext context) {
        Objects.requireNonNull(context, "context must not be null");
        long start = System.currentTimeMillis();
        BudgetStatistics stats = new BudgetStatistics();

        // 1. Unchanged structural hash complete reuse check
        if (context.isIncremental() && incrementalManager.isUnchanged(context.rankingResult().projectId(), context.structuralHash())) {
            BudgetResult cached = incrementalManager.getCachedResult(context.rankingResult().projectId());
            if (cached != null) {
                return cached;
            }
        }

        // 2. Incremental partial cache check
        Map<String, BudgetedFragment> cacheMap = Collections.emptyMap();
        if (context.isIncremental()) {
            cacheMap = incrementalManager.getCachedFragments(context.rankingResult().projectId());
        }

        List<RankedFragment> rankedFragments = context.rankingResult().rankedFragments();
        List<BudgetedFragment> budgeted = new ArrayList<>();
        BudgetState state = new BudgetState();

        for (RankedFragment rf : rankedFragments) {
            stats.incrementFragmentsEvaluated(1);

            BudgetedFragment bf = null;
            // If incremental and fragment source path is not dirty, try to reuse cached allocation
            if (context.isIncremental() && !context.dirtyPaths().contains(rf.fragment().sourcePath())) {
                bf = cacheMap.get(rf.fragment().fragmentId());
            }

            if (bf != null) {
                // If it is reserved/included, update accumulators based on cached allocation
                int charLength = rf.fragment().content() != null ? rf.fragment().content().length() : 0;
                int estimatedTokens = Math.max(1, charLength / 4);

                if (bf.decision() == BudgetDecision.RESERVED) {
                    state.incrementCharacters(charLength);
                    state.incrementTokens(estimatedTokens);
                    stats.incrementReservedFragments(1);
                    stats.incrementFragmentsIncluded(1);
                } else if (bf.decision() == BudgetDecision.INCLUDED) {
                    state.incrementFragments(1);
                    state.incrementCharacters(charLength);
                    state.incrementTokens(estimatedTokens);
                    stats.incrementFragmentsIncluded(1);
                } else if (bf.decision() == BudgetDecision.PARTIALLY_INCLUDED) {
                    state.incrementFragments(1);
                    state.incrementCharacters(charLength);
                    state.incrementTokens(estimatedTokens);
                    stats.incrementPartiallyIncluded(1);
                    stats.incrementFragmentsIncluded(1);
                } else {
                    stats.incrementFragmentsExcluded(1);
                }
                budgeted.add(new BudgetedFragment(rf, bf.decision(), bf.reason()));
            } else {
                // Compute fresh
                for (BudgetAlgorithm strategy : strategies) {
                    bf = strategy.allocate(rf, context.configuration(), state);
                    if (bf != null) {
                        break;
                    }
                }
                if (bf == null) {
                    bf = new BudgetedFragment(rf, BudgetDecision.EXCLUDED, BudgetReason.NONE);
                }

                // Update stats
                if (bf.decision() == BudgetDecision.RESERVED) {
                    stats.incrementReservedFragments(1);
                    stats.incrementFragmentsIncluded(1);
                } else if (bf.decision() == BudgetDecision.INCLUDED) {
                    stats.incrementFragmentsIncluded(1);
                } else if (bf.decision() == BudgetDecision.PARTIALLY_INCLUDED) {
                    stats.incrementPartiallyIncluded(1);
                    stats.incrementFragmentsIncluded(1);
                } else {
                    stats.incrementFragmentsExcluded(1);
                }

                budgeted.add(bf);
            }
        }

        stats.incrementEstimatedCharactersUsed(state.accumulatedCharacters());
        stats.incrementEstimatedTokensUsed(state.accumulatedTokens());

        // 3. Validation execution
        long validateStart = System.currentTimeMillis();
        BudgetValidator validator = new BudgetValidator();
        BudgetValidationResult validationResult = validator.validate(budgeted, context.configuration());
        stats.setValidationDuration(System.currentTimeMillis() - validateStart);

        if (!validationResult.isValid()) {
            stats.incrementWarnings(validationResult.errors().size());
        }

        stats.setProcessingDuration(System.currentTimeMillis() - start);

        BudgetResult result = new BudgetResult(
            context.rankingResult().projectId(),
            budgeted,
            stats,
            Instant.now()
        );

        // Cache the computed state for future incremental runs
        incrementalManager.cacheResult(context.rankingResult().projectId(), context.structuralHash(), result);

        return result;
    }
}
