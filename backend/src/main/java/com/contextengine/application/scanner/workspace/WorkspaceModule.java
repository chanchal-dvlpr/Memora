package com.contextengine.application.scanner.workspace;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a single submodule or subproject discovered within a workspace.
 */
public class WorkspaceModule {
    private final String moduleId;
    private final String moduleName;
    private final String relativePath;
    private final String buildSystem;
    private final String detectedLanguage;

    /**
     * Constructs a WorkspaceModule.
     *
     * @param moduleName name of the workspace module
     * @param relativePath relative path to the module from workspace root
     * @param buildSystem build system identifier (e.g. pnpm, npm, maven, gradle)
     * @param detectedLanguage detected primary programming language of the module
     */
    public WorkspaceModule(String moduleName, String relativePath, String buildSystem, String detectedLanguage) {
        this.moduleId = UUID.randomUUID().toString();
        this.moduleName = Objects.requireNonNull(moduleName, "ModuleName must not be null");
        this.relativePath = Objects.requireNonNull(relativePath, "RelativePath must not be null");
        this.buildSystem = Objects.requireNonNull(buildSystem, "BuildSystem must not be null");
        this.detectedLanguage = Objects.requireNonNull(detectedLanguage, "DetectedLanguage must not be null");
    }

    public String moduleId() {
        return moduleId;
    }

    public String moduleName() {
        return moduleName;
    }

    public String relativePath() {
        return relativePath;
    }

    public String buildSystem() {
        return buildSystem;
    }

    public String detectedLanguage() {
        return detectedLanguage;
    }
}
