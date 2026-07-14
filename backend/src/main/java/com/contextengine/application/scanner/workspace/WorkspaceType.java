package com.contextengine.application.scanner.workspace;

/**
 * Enumerates the supported workspace and monorepo configuration formats.
 */
public enum WorkspaceType {
    NONE,
    PNPM,
    NPM,
    NX,
    TURBO,
    LERNA,
    RUSH,
    MAVEN,
    GRADLE
}
