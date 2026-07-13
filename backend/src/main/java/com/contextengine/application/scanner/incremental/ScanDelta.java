package com.contextengine.application.scanner.incremental;

import com.contextengine.application.scanner.ScanCandidate;
import java.util.List;
import java.util.Objects;

/**
 * Grouping structure representing the detected added, modified, and deleted scan candidates.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * </p>
 */
public record ScanDelta(
    List<ScanCandidate> added,
    List<ScanCandidate> modified,
    List<String> deleted
) {
    /**
     * Constructs a ScanDelta.
     *
     * @param added list of newly added candidates
     * @param modified list of modified candidates
     * @param deleted list of deleted relative paths
     */
    public ScanDelta {
        Objects.requireNonNull(added, "Added list must not be null");
        Objects.requireNonNull(modified, "Modified list must not be null");
        Objects.requireNonNull(deleted, "Deleted list must not be null");
    }
}
