package com.contextengine.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * REST request model representing a request to register a new project directory.
 */
public class RegisterProjectRequest {

    @NotBlank(message = "Project name is required")
    @Size(min = 1, max = 255, message = "Project name must be between 1 and 255 characters")
    private String name;

    @NotBlank(message = "Root path is required")
    private String rootPath;

    private List<String> exclusionPatterns;

    /**
     * Default constructor for serialization.
     */
    public RegisterProjectRequest() {
    }

    /**
     * Constructs a RegisterProjectRequest with all attributes.
     *
     * @param name project display title
     * @param rootPath absolute path to the project directory
     * @param exclusionPatterns list of exclusions patterns
     */
    public RegisterProjectRequest(String name, String rootPath, List<String> exclusionPatterns) {
        this.name = name;
        this.rootPath = rootPath;
        this.exclusionPatterns = exclusionPatterns;
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
}
