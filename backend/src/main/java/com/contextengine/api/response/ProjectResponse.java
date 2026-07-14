package com.contextengine.api.response;

import java.util.List;

/**
 * REST response model representing registered Project metadata.
 */
public class ProjectResponse {

    private String id;
    private String name;
    private String rootPath;
    private List<String> exclusionPatterns;
    private String status;
    private String createdAt;
    private int version;

    /**
     * Default constructor for deserialization.
     */
    public ProjectResponse() {
    }

    /**
     * Constructs a ProjectResponse.
     *
     * @param id project unique identifier
     * @param name project display title
     * @param rootPath absolute root path directory
     * @param exclusionPatterns file ignore patterns
     * @param status operational phase indexing state
     * @param createdAt registration timestamp string
     * @param version optimistic locking configuration version
     */
    public ProjectResponse(String id, String name, String rootPath, List<String> exclusionPatterns, String status, String createdAt, int version) {
        this.id = id;
        this.name = name;
        this.rootPath = rootPath;
        this.exclusionPatterns = exclusionPatterns;
        this.status = status;
        this.createdAt = createdAt;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public List<String> getExclusionPatterns() {
        return exclusionPatterns;
    }

    public void setExclusionPatterns(List<String> exclusionPatterns) {
        this.exclusionPatterns = exclusionPatterns;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
