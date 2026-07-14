package com.contextengine.application.knowledge.snapshot;

import com.contextengine.domain.entity.ContextSnapshot;
import com.contextengine.domain.valueobject.EngineeringEvidence;
import java.time.Instant;
import java.util.*;

/**
 * Strategy-driven implementation of SnapshotComparisonEngine integrating validation and caching.
 */
public class SnapshotComparisonEngineImpl implements SnapshotComparisonEngine {

    private final List<SnapshotComparisonAlgorithm> strategies = List.of(
        new ExactPathComparisonStrategy(),
        new RenameDetectionStrategy(),
        new MoveDetectionStrategy()
    );

    private final IncrementalSnapshotComparisonEngine incrementalEngine = new IncrementalSnapshotComparisonEngine();

    @Override
    public SnapshotComparisonResult compare(SnapshotComparisonContext context) {
        Objects.requireNonNull(context, "context must not be null");
        long start = System.currentTimeMillis();
        SnapshotComparisonStatistics stats = new SnapshotComparisonStatistics();
        SnapshotComparisonConfiguration config = context.configuration();

        ContextSnapshot current = context.currentSnapshot();
        ContextSnapshot previous = context.previousSnapshot();

        // 1. Unchanged cache check
        if (context.isIncremental() && incrementalEngine.isUnchanged(current.projectId().value().toString(), context.structuralHash())) {
            SnapshotComparisonResult cached = incrementalEngine.getCachedResult(current.projectId().value().toString());
            if (cached != null) {
                return cached;
            }
        }

        List<SnapshotDifference> differences = new ArrayList<>();

        int added = 0;
        int removed = 0;
        int modified = 0;
        int renamed = 0;
        int moved = 0;
        int unchanged = 0;

        if (previous == null) {
            // First run, everything is added
            for (EngineeringEvidence curr : current.evidences()) {
                differences.add(new SnapshotDifference(SnapshotDifferenceType.ADDED, null, curr, "Initial addition of entity"));
                added++;
                stats.incrementEntitiesCompared(1);
                stats.incrementDifferencesFound(1);
            }
        } else {
            List<EngineeringEvidence> prevEvidences = previous.evidences();
            List<EngineeringEvidence> currEvidences = current.evidences();

            Set<EngineeringEvidence> matchedPrev = new HashSet<>();
            Set<EngineeringEvidence> matchedCurr = new HashSet<>();

            // Run strategies sequentially
            for (SnapshotComparisonAlgorithm strategy : strategies) {
                strategy.compare(prevEvidences, currEvidences, config, differences, matchedPrev, matchedCurr);
            }

            // Removals (previous not matched)
            for (EngineeringEvidence p : prevEvidences) {
                if (!matchedPrev.contains(p)) {
                    differences.add(new SnapshotDifference(SnapshotDifferenceType.REMOVED, p, null, "Entity was removed"));
                    removed++;
                    stats.incrementDifferencesFound(1);
                }
            }

            // Additions (current not matched)
            for (EngineeringEvidence c : currEvidences) {
                if (!matchedCurr.contains(c)) {
                    differences.add(new SnapshotDifference(SnapshotDifferenceType.ADDED, null, c, "Entity was added"));
                    added++;
                    stats.incrementDifferencesFound(1);
                }
            }

            // Accumulate statistics and summary counts from differences
            for (SnapshotDifference diff : differences) {
                switch (diff.differenceType()) {
                    case UNCHANGED:
                        unchanged++;
                        break;
                    case MODIFIED:
                        modified++;
                        stats.incrementDifferencesFound(1);
                        break;
                    case RENAMED:
                        renamed++;
                        stats.incrementDifferencesFound(1);
                        break;
                    case MOVED:
                        moved++;
                        stats.incrementDifferencesFound(1);
                        break;
                }
            }
            stats.incrementEntitiesCompared(currEvidences.size());
        }

        SnapshotComparisonSummary summary = new SnapshotComparisonSummary(
            added, removed, modified, renamed, moved, unchanged
        );

        stats.incrementAdditions(added);
        stats.incrementRemovals(removed);
        stats.incrementModifications(modified);
        stats.incrementRenames(renamed);
        stats.incrementMoves(moved);
        stats.incrementUnchanged(unchanged);

        // 2. Validation execution
        long validateStart = System.currentTimeMillis();
        SnapshotComparisonValidator validator = new SnapshotComparisonValidator();
        SnapshotComparisonValidationResult validationResult = validator.validate(differences, summary);
        stats.setValidationDuration(System.currentTimeMillis() - validateStart);

        if (!validationResult.isValid()) {
            stats.incrementWarnings(validationResult.errors().size());
        }

        stats.setComparisonDuration(System.currentTimeMillis() - start);

        SnapshotComparisonResult result = new SnapshotComparisonResult(
            current.projectId().value().toString(),
            differences,
            summary,
            stats,
            Instant.now()
        );

        // Cache the completed state
        incrementalEngine.cacheResult(current.projectId().value().toString(), context.structuralHash(), result);

        return result;
    }
}
