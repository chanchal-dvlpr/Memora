package com.contextengine.domain.entity;

import com.contextengine.domain.valueobject.DependencyId;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.SemanticVersion;
import java.util.Objects;

/**
 * Models external modules, frameworks, and APIs consumed by the workspace.
 */
public class Dependency {
    
    private final DependencyId id;
    private final ProjectId projectId;
    private final String packageName;
    private SemanticVersion version;
    private final Path manifestPath;

    /**
     * Constructs a Dependency.
     *
     * @param id the unique dependency ID
     * @param projectId the parent project ID
     * @param packageName the name of the package/library
     * @param version the semantic version of the dependency
     * @param manifestPath the path to the manifest file declaring this dependency
     */
    public Dependency(DependencyId id, ProjectId projectId, String packageName, SemanticVersion version, Path manifestPath) {
        this.id = Objects.requireNonNull(id, "DependencyId must not be null");
        this.projectId = Objects.requireNonNull(projectId, "ProjectId must not be null");
        this.version = Objects.requireNonNull(version, "SemanticVersion must not be null");
        this.manifestPath = Objects.requireNonNull(manifestPath, "Manifest path must not be null");
        
        if (packageName == null || packageName.trim().isEmpty()) {
            throw new IllegalArgumentException("Package name must not be null or empty");
        }
        this.packageName = packageName.trim();
    }

    public DependencyId id() {
        return id;
    }

    public ProjectId projectId() {
        return projectId;
    }

    public String packageName() {
        return packageName;
    }

    public SemanticVersion version() {
        return version;
    }

    public Path manifestPath() {
        return manifestPath;
    }

    /**
     * Upgrades/changes the semantic version of this dependency.
     *
     * @param newVersion the new version to set
     */
    public void upgradeVersion(SemanticVersion newVersion) {
        this.version = Objects.requireNonNull(newVersion, "New version must not be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dependency that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
