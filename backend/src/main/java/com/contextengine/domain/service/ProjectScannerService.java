package com.contextengine.domain.service;

import com.contextengine.domain.entity.Project;
import com.contextengine.domain.entity.Workspace;
import com.contextengine.domain.valueobject.Path;
import java.util.Collection;
import java.util.Objects;

/**
 * Coordinates directory parsing, reads codebase constructs, and translates structures.
 */
public class ProjectScannerService {
    
    /**
     * Traverses and tracks discovered paths inside the project workspace.
     *
     * @param project the project to scan
     * @param discoveredPaths the paths parsed during scanning
     * @throws NullPointerException if any argument is null
     */
    public void scanWorkspace(Project project, Collection<Path> discoveredPaths) {
        Objects.requireNonNull(project, "Project must not be null");
        Objects.requireNonNull(discoveredPaths, "Discovered paths must not be null");
        
        Workspace workspace = project.workspace();
        if (workspace == null) {
            throw new IllegalStateException("Workspace must be bound to project before scanning");
        }
        
        for (Path path : discoveredPaths) {
            workspace.trackPath(path);
        }
    }
}
