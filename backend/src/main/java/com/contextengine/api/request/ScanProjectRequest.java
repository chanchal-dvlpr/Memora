package com.contextengine.api.request;

/**
 * REST request model representing a manual trigger request for project scanning.
 */
public class ScanProjectRequest {

    private boolean deep;

    /**
     * Default constructor for serialization.
     */
    public ScanProjectRequest() {
    }

    /**
     * Constructs a ScanProjectRequest.
     *
     * @param deep true for a full AST index traversal, false for incremental updates
     */
    public ScanProjectRequest(boolean deep) {
        this.deep = deep;
    }

    public boolean isDeep() {
        return deep;
    }

    public void setDeep(boolean deep) {
        this.deep = deep;
    }
}
