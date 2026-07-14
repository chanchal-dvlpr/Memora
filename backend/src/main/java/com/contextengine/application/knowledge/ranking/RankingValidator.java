package com.contextengine.application.knowledge.ranking;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Structural validator ensuring ranked fragments have correct ordering, unique IDs, non-negative scores, and valid reasons.
 */
public class RankingValidator {

    /**
     * Runs structural validation checks.
     *
     * @param rankedFragments list of ranked fragments
     * @param config          ranking configuration rules
     * @return validation result holding success status and error messages
     */
    public RankingValidationResult validate(List<RankedFragment> rankedFragments, RankingConfiguration config) {
        Objects.requireNonNull(rankedFragments, "rankedFragments must not be null");
        Objects.requireNonNull(config, "config must not be null");

        List<String> errors = new ArrayList<>();

        if (config.maximumCandidates() <= 0) {
            errors.add("maximumCandidates must be greater than 0");
        }

        Set<String> seenIds = new HashSet<>();
        RankedFragment previous = null;

        for (int i = 0; i < rankedFragments.size(); i++) {
            RankedFragment current = rankedFragments.get(i);
            if (current == null) {
                errors.add("RankedFragment at index " + i + " is null");
                continue;
            }

            if (current.fragment() == null) {
                errors.add("ContextFragment at index " + i + " is null");
            } else {
                String fid = current.fragment().fragmentId();
                if (!seenIds.add(fid)) {
                    errors.add("Duplicate RankedFragment ID detected: " + fid);
                }
            }

            RankingScore score = current.score();
            if (score == null) {
                errors.add("RankingScore at index " + i + " is null");
            } else {
                double val = score.value();
                if (Double.isNaN(val) || Double.isInfinite(val)) {
                    errors.add("Invalid score value (NaN/Infinite) at index " + i);
                }
                if (val < 0) {
                    errors.add("Score value is negative at index " + i + ": " + val);
                }
                if (score.reasons().isEmpty()) {
                    errors.add("No ranking reasons provided at index " + i);
                }
                for (RankingReason reason : score.reasons()) {
                    if (reason.contribution() < 0) {
                        errors.add("Negative contribution factor: " + reason.factor() + " in fragment index " + i);
                    }
                }
            }

            // Ordering validation: asserts descending order
            if (previous != null) {
                if (previous.compareTo(current) > 0) {
                    errors.add("Inconsistent ordering: fragment at index " + (i - 1) + " must precede or equal fragment at index " + i);
                }
            }
            previous = current;
        }

        return new RankingValidationResult(errors.isEmpty(), errors);
    }
}
