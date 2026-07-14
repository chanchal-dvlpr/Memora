package com.contextengine.application.knowledge.snapshot;

import com.contextengine.domain.valueobject.EngineeringEvidence;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Validator evaluating relational integrity and consistency of snapshot comparison outputs.
 */
public class SnapshotComparisonValidator {

    /**
     * Validates snapshot comparison differences and summaries.
     *
     * @param differences list of comparison differences
     * @param summary     comparison summary
     * @return validation result
     */
    public SnapshotComparisonValidationResult validate(
        List<SnapshotDifference> differences,
        SnapshotComparisonSummary summary
    ) {
        List<String> errors = new ArrayList<>();

        if (differences == null) {
            errors.add("differences list is null");
            return new SnapshotComparisonValidationResult(false, errors);
        }
        if (summary == null) {
            errors.add("summary is null");
            return new SnapshotComparisonValidationResult(false, errors);
        }

        Set<String> prevPaths = new HashSet<>();
        Set<String> currPaths = new HashSet<>();

        int addedCount = 0;
        int removedCount = 0;
        int modifiedCount = 0;
        int renamedCount = 0;
        int movedCount = 0;
        int unchangedCount = 0;

        for (int i = 0; i < differences.size(); i++) {
            SnapshotDifference diff = differences.get(i);
            if (diff == null) {
                errors.add("SnapshotDifference at index " + i + " is null");
                continue;
            }

            SnapshotDifferenceType type = diff.differenceType();
            if (type == null) {
                errors.add("Difference type at index " + i + " is null");
                continue;
            }

            EngineeringEvidence prev = diff.previousEvidence();
            EngineeringEvidence curr = diff.currentEvidence();

            // Validate duplicate differences and references
            if (prev != null) {
                String prevPath = prev.filePath().value();
                if (!prevPaths.add(prevPath)) {
                    errors.add("Duplicate previous evidence path reference: " + prevPath);
                }
            }
            if (curr != null) {
                String currPath = curr.filePath().value();
                if (!currPaths.add(currPath)) {
                    errors.add("Duplicate current evidence path reference: " + currPath);
                }
            }

            switch (type) {
                case ADDED:
                    addedCount++;
                    if (curr == null) {
                        errors.add("ADDED difference at index " + i + " has null currentEvidence");
                    }
                    if (prev != null) {
                        errors.add("ADDED difference at index " + i + " has non-null previousEvidence");
                    }
                    break;
                case REMOVED:
                    removedCount++;
                    if (prev == null) {
                        errors.add("REMOVED difference at index " + i + " has null previousEvidence");
                    }
                    if (curr != null) {
                        errors.add("REMOVED difference at index " + i + " has non-null currentEvidence");
                    }
                    break;
                case UNCHANGED:
                    unchangedCount++;
                    if (prev == null || curr == null) {
                        errors.add("UNCHANGED difference at index " + i + " has null references");
                    }
                    break;
                case MODIFIED:
                    modifiedCount++;
                    if (prev == null || curr == null) {
                        errors.add("MODIFIED difference at index " + i + " has null references");
                    }
                    break;
                case RENAMED:
                    renamedCount++;
                    if (prev == null || curr == null) {
                        errors.add("RENAMED difference at index " + i + " has null references");
                    } else if (!Objects.equals(prev.fileContentHash().value(), curr.fileContentHash().value())) {
                        errors.add("RENAMED difference at index " + i + " has mismatched hashes: " 
                            + prev.fileContentHash().value() + " vs " + curr.fileContentHash().value());
                    }
                    break;
                case MOVED:
                    movedCount++;
                    if (prev == null || curr == null) {
                        errors.add("MOVED difference at index " + i + " has null references");
                    } else if (!Objects.equals(prev.fileContentHash().value(), curr.fileContentHash().value())) {
                        errors.add("MOVED difference at index " + i + " has mismatched hashes: " 
                            + prev.fileContentHash().value() + " vs " + curr.fileContentHash().value());
                    }
                    break;
            }
        }

        // Validate inconsistent summaries
        if (addedCount != summary.added()) {
            errors.add("Inconsistent ADDED summary count: expected " + addedCount + " but got " + summary.added());
        }
        if (removedCount != summary.removed()) {
            errors.add("Inconsistent REMOVED summary count: expected " + removedCount + " but got " + summary.removed());
        }
        if (modifiedCount != summary.modified()) {
            errors.add("Inconsistent MODIFIED summary count: expected " + modifiedCount + " but got " + summary.modified());
        }
        if (renamedCount != summary.renamed()) {
            errors.add("Inconsistent RENAMED summary count: expected " + renamedCount + " but got " + summary.renamed());
        }
        if (movedCount != summary.moved()) {
            errors.add("Inconsistent MOVED summary count: expected " + movedCount + " but got " + summary.moved());
        }
        if (unchangedCount != summary.unchanged()) {
            errors.add("Inconsistent UNCHANGED summary count: expected " + unchangedCount + " but got " + summary.unchanged());
        }

        return new SnapshotComparisonValidationResult(errors.isEmpty(), errors);
    }
}
