package com.contextengine.api.response;

/**
 * REST response model representing the compiled prompt context payload output.
 */
public class ContextResponse {

    private String contextId;
    private String projectId;
    private int totalTokensConsumed;
    private String assembledTextPayload;
    private String timestamp;

    /**
     * Default constructor for deserialization.
     */
    public ContextResponse() {
    }

    /**
     * Constructs a ContextResponse.
     *
     * @param contextId unique context snapshot identifier
     * @param projectId associated parent project identifier
     * @param totalTokensConsumed token size consumed by the prompt payload
     * @param assembledTextPayload compiled formatted prompt context string
     * @param timestamp assembly ISO-8601 timestamp string
     */
    public ContextResponse(String contextId, String projectId, int totalTokensConsumed, String assembledTextPayload, String timestamp) {
        this.contextId = contextId;
        this.projectId = projectId;
        this.totalTokensConsumed = totalTokensConsumed;
        this.assembledTextPayload = assembledTextPayload;
        this.timestamp = timestamp;
    }

    public String getContextId() {
        return contextId;
    }

    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public int getTotalTokensConsumed() {
        return totalTokensConsumed;
    }

    public void setTotalTokensConsumed(int totalTokensConsumed) {
        this.totalTokensConsumed = totalTokensConsumed;
    }

    public String getAssembledTextPayload() {
        return assembledTextPayload;
    }

    public void setAssembledTextPayload(String assembledTextPayload) {
        this.assembledTextPayload = assembledTextPayload;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
