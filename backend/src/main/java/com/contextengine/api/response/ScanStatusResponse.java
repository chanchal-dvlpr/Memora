package com.contextengine.api.response;

/**
 * REST response model representing the progress and configuration state of a scanner.
 */
public class ScanStatusResponse {

    private String scanJobId;
    private String projectId;
    private String status;
    private String triggeredAt;
    private boolean watcherActive;
    private long filesProcessed;

    /**
     * Default constructor for deserialization.
     */
    public ScanStatusResponse() {
    }

    /**
     * Constructs a ScanStatusResponse.
     *
     * @param scanJobId unique identifier for the scan run job
     * @param projectId associated parent project identifier
     * @param status job execution status
     * @param triggeredAt scan start timestamp string
     * @param watcherActive flag representing filesystem watcher activation state
     * @param filesProcessed count of files successfully parsed in workspace
     */
    public ScanStatusResponse(String scanJobId, String projectId, String status, String triggeredAt, boolean watcherActive, long filesProcessed) {
        this.scanJobId = scanJobId;
        this.projectId = projectId;
        this.status = status;
        this.triggeredAt = triggeredAt;
        this.watcherActive = watcherActive;
        this.filesProcessed = filesProcessed;
    }

    public String getScanJobId() {
        return scanJobId;
    }

    public void setScanJobId(String scanJobId) {
        this.scanJobId = scanJobId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTriggeredAt() {
        return triggeredAt;
    }

    public void setTriggeredAt(String triggeredAt) {
        this.triggeredAt = triggeredAt;
    }

    public boolean isWatcherActive() {
        return watcherActive;
    }

    public void setWatcherActive(boolean watcherActive) {
        this.watcherActive = watcherActive;
    }

    public long getFilesProcessed() {
        return filesProcessed;
    }

    public void setFilesProcessed(long filesProcessed) {
        this.filesProcessed = filesProcessed;
    }
}
