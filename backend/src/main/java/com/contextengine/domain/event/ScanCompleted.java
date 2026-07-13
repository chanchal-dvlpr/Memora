package com.contextengine.domain.event;

import com.contextengine.domain.valueobject.ProjectId;
import java.time.Instant;
import java.util.Objects;

/**
 * Occurs when a scanning operation completes successfully on a project workspace.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Event Subsystem (EV-SUB)
 * </p>
 */
public record ScanCompleted(
    ProjectId projectId,
    String scanMode,
    int filesScannedCount,
    int symbolsCount,
    Instant occurredAt
) implements DomainEvent {

    /**
     * Constructs a ScanCompleted event.
     *
     * @param projectId target project ID
     * @param scanMode scan modality (FULL or INCREMENTAL)
     * @param filesScannedCount total count of files scanned
     * @param symbolsCount total count of symbols extracted
     * @param occurredAt event occurrence timestamp
     */
    public ScanCompleted {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(scanMode, "ScanMode must not be null");
        Objects.requireNonNull(occurredAt, "OccurredAt must not be null");
        if (filesScannedCount < 0) {
            throw new IllegalArgumentException("Files scanned count must be non-negative");
        }
        if (symbolsCount < 0) {
            throw new IllegalArgumentException("Symbols count must be non-negative");
        }
    }
}
