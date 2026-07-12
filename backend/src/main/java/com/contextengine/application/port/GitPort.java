package com.contextengine.application.port;

import com.contextengine.domain.valueobject.Path;

/**
 * Port interface abstracting Git version control operations.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Git Monitoring
 * </p>
 */
public interface GitPort {
    
    /**
     * Checks if the specified directory contains an initialized Git repository.
     *
     * @param directory the directory path
     * @return true if Git is initialized, false otherwise
     */
    boolean isGitRepository(Path directory);

    /**
     * Retrieves the name of the active branch in the repository.
     *
     * @param directory the repository root directory path
     * @return the branch name, or empty if unavailable
     */
    String getActiveBranch(Path directory);

    /**
     * Retrieves the latest commit hash in the repository.
     *
     * @param directory the repository root directory path
     * @return the commit hash, or empty if unavailable
     */
    String getLatestCommitHash(Path directory);
}
