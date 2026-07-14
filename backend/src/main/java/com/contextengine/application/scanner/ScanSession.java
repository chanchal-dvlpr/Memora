package com.contextengine.application.scanner;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Tracks the execution state and gathered metrics of a single workspace scanning session.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * </p>
 */
public class ScanSession {

    /**
     * Enum defining the scanner processing states.
     */
    public enum State {
        IDLE,
        QUEUED,
        SCANNING,
        COMPLETED,
        FAILED
    }

    private final UUID scanId;
    private final UUID projectId;
    private final String scanMode; // FULL or INCREMENTAL
    private State state;
    private final Instant startTime;
    private Instant endTime;

    private long directoryCount = 0;
    private long fileCount = 0;
    private long byteCount = 0;

    /**
     * Constructs a ScanSession.
     *
     * @param projectId target project identifier
     * @param scanMode scan modality (FULL or INCREMENTAL)
     */
    public ScanSession(UUID projectId, String scanMode) {
        this.scanId = UUID.randomUUID();
        this.projectId = Objects.requireNonNull(projectId, "ProjectId must not be null");
        this.scanMode = Objects.requireNonNull(scanMode, "ScanMode must not be null");
        this.state = State.IDLE;
        this.startTime = Instant.now();
    }

    public UUID getScanId() {
        return scanId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getScanMode() {
        return scanMode;
    }

    public State getState() {
        return state;
    }

    public void transitionTo(State newState) {
        this.state = Objects.requireNonNull(newState, "State must not be null");
        if (newState == State.COMPLETED || newState == State.FAILED) {
            this.endTime = Instant.now();
        }
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public long getDirectoryCount() {
        return directoryCount;
    }

    public void incrementDirectories() {
        this.directoryCount++;
    }

    public long getFileCount() {
        return fileCount;
    }

    public void incrementFiles() {
        this.fileCount++;
    }

    public long getByteCount() {
        return byteCount;
    }

    public void addBytes(long bytes) {
        this.byteCount += bytes;
    }

    /**
     * Resolves duration in milliseconds.
     *
     * @return duration, or 0 if scan has not ended
     */
    public long getDurationMs() {
        if (endTime == null) {
            return Instant.now().toEpochMilli() - startTime.toEpochMilli();
        }
        return endTime.toEpochMilli() - startTime.toEpochMilli();
    }

    private String compositeStructuralHash;

    public String getCompositeStructuralHash() {
        return compositeStructuralHash;
    }

    public void setCompositeStructuralHash(String hash) {
        this.compositeStructuralHash = hash;
    }
}
