package com.contextengine.application.scanner.workspace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Container holding monorepo characteristics, root references, and discovered submodules.
 */
public class MonorepoDescriptor {
    private final WorkspaceType workspaceType;
    private final String rootPath;
    private final List<WorkspaceModule> detectedModules;

    /**
     * Constructs a MonorepoDescriptor.
     *
     * @param workspaceType build system type
     * @param rootPath canonical root workspace path
     * @param detectedModules list of resolved modules
     */
    public MonorepoDescriptor(WorkspaceType workspaceType, String rootPath, List<WorkspaceModule> detectedModules) {
        this.workspaceType = Objects.requireNonNull(workspaceType, "WorkspaceType must not be null");
        this.rootPath = Objects.requireNonNull(rootPath, "RootPath must not be null");
        this.detectedModules = new ArrayList<>(Objects.requireNonNull(detectedModules, "DetectedModules must not be null"));
    }

    public WorkspaceType workspaceType() {
        return workspaceType;
    }

    public String rootPath() {
        return rootPath;
    }

    public List<WorkspaceModule> detectedModules() {
        return Collections.unmodifiableList(detectedModules);
    }

    public List<String> moduleNames() {
        return detectedModules.stream()
                .map(WorkspaceModule::moduleName)
                .toList();
    }

    public List<String> modulePaths() {
        return detectedModules.stream()
                .map(WorkspaceModule::relativePath)
                .toList();
    }
}
