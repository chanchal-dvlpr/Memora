package com.contextengine.domain.event;

import com.contextengine.domain.valueobject.ProjectId;
import java.time.Instant;
import java.util.Objects;

/**
 * Occurs when a scanning operation starts on a project workspace.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Event Subsystem (EV-SUB)
 * </p>
 */
public record ScanStarted(
    ProjectId projectId,
    String scanMode,
    Instant occurredAt
) implements DomainEvent {

    /**
     * Constructs a ScanStarted event.
     *
     * @param projectId target project ID
     * @param scanMode scan modality (FULL or INCREMENTAL)
     * @param occurredAt event occurrence timestamp
     */
    public ScanStarted {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(scanMode, "ScanMode must not be null");
        Objects.requireNonNull(occurredAt, "OccurredAt must not be null");
    }
}
