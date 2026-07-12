package com.contextengine.domain.entity;

import com.contextengine.domain.valueobject.ModuleId;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import java.util.Objects;

/**
 * Represents a logical, architectural subsystem, layer, package, or directory-bounded component inside a Project.
 */
public class Module {
    
    private final ModuleId id;
    private final ProjectId projectId;
    private String name;
    private Path relativePath;

    /**
     * Constructs a Module.
     *
     * @param id the unique module ID
     * @param projectId the parent project ID
     * @param name the name of the module
     * @param relativePath the relative directory path of the module
     */
    public Module(ModuleId id, ProjectId projectId, String name, Path relativePath) {
        this.id = Objects.requireNonNull(id, "ModuleId must not be null");
        this.projectId = Objects.requireNonNull(projectId, "ProjectId must not be null");
        this.relativePath = Objects.requireNonNull(relativePath, "Relative path must not be null");
        
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Module name must not be null or empty");
        }
        if (name.contains("/") || name.contains("\\")) {
            throw new IllegalArgumentException("Module name must not contain path separators");
        }
        this.name = name.trim();
    }

    public ModuleId id() {
        return id;
    }

    public ProjectId projectId() {
        return projectId;
    }

    public String name() {
        return name;
    }

    public Path relativePath() {
        return relativePath;
    }

    /**
     * Renames the module.
     *
     * @param newName the new name of the module
     */
    public void rename(String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Module name must not be null or empty");
        }
        if (newName.contains("/") || newName.contains("\\")) {
            throw new IllegalArgumentException("Module name must not contain path separators");
        }
        this.name = newName.trim();
    }

    /**
     * Updates the relative path boundary of the module.
     *
     * @param newPath the new relative path
     */
    public void updatePath(Path newPath) {
        this.relativePath = Objects.requireNonNull(newPath, "New relative path must not be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Module module)) return false;
        return id.equals(module.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
