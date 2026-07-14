package com.contextengine.api.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Pattern;

/**
 * REST request model representing parameters for generating a prompt context snapshot.
 */
public class GenerateContextRequest {

    @NotBlank(message = "Project ID is required")
    @Pattern(
        regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
        message = "Project ID must be a valid UUID format"
    )
    private String projectId;
    
    private String query;
    private String focusFile;

    @Min(value = 1000, message = "Token budget must be at least 1000")
    @Max(value = 1000000, message = "Token budget must not exceed 1000000")
    private int maxTokenBudget;
    
    private String format = "MARKDOWN";

    /**
     * Default constructor for serialization.
     */
    public GenerateContextRequest() {
    }

    /**
     * Constructs a GenerateContextRequest.
     *
     * @param projectId parent project identifier
     * @param query search query term
     * @param focusFile targeted focal file path
     * @param maxTokenBudget maximum allowable token footprint
     * @param format target format specification
     */
    public GenerateContextRequest(String projectId, String query, String focusFile, int maxTokenBudget, String format) {
        this.projectId = projectId;
        this.query = query;
        this.focusFile = focusFile;
        this.maxTokenBudget = maxTokenBudget;
        this.format = format;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getFocusFile() {
        return focusFile;
    }

    public void setFocusFile(String focusFile) {
        this.focusFile = focusFile;
    }

    public int getMaxTokenBudget() {
        return maxTokenBudget;
    }

    public void setMaxTokenBudget(int maxTokenBudget) {
        this.maxTokenBudget = maxTokenBudget;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
