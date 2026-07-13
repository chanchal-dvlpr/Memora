package com.contextengine.application.scanner.dependency;

import java.util.Objects;

/**
 * Represents a normalized dependency extracted from a project manifest or source file.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * </p>
 */
public record ProjectDependency(
    String name,
    String version,
    String type,
    String scope
) {
    /**
     * Constructs a ProjectDependency.
     *
     * @param name dependency package name
     * @param version dependency version
     * @param type dependency type (e.g. MAVEN, NPM, PIP)
     * @param scope dependency scope (e.g. COMPILE, TEST, RUNTIME)
     */
    public ProjectDependency {
        Objects.requireNonNull(name, "Dependency name must not be null");
        Objects.requireNonNull(version, "Dependency version must not be null");
        Objects.requireNonNull(type, "Dependency type must not be null");
        Objects.requireNonNull(scope, "Dependency scope must not be null");
    }
}
