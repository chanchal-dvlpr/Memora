package com.contextengine.application.knowledge.snapshot;

import com.contextengine.domain.valueobject.EngineeringEvidence;
import java.util.List;
import java.util.Set;

/**
 * Strategy interface for comparing snapshot entity collections.
 */
public interface SnapshotComparisonAlgorithm {

    /**
     * Executes matching comparisons between previous and current evidence lists.
     *
     * @param prev        previous evidence list
     * @param curr        current evidence list
     * @param config      comparison configuration toggles
     * @param differences results list to accumulate found differences
     * @param matchedPrev set of matched previous elements
     * @param matchedCurr set of matched current elements
     */
    void compare(
        List<EngineeringEvidence> prev,
        List<EngineeringEvidence> curr,
        SnapshotComparisonConfiguration config,
        List<SnapshotDifference> differences,
        Set<EngineeringEvidence> matchedPrev,
        Set<EngineeringEvidence> matchedCurr
    );
}
