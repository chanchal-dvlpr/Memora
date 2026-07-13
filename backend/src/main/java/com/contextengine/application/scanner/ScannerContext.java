package com.contextengine.application.scanner;

import com.contextengine.domain.entity.Project;
import com.contextengine.domain.valueobject.Path;
import java.util.Objects;

/**
 * Encapsulates the execution context of a workspace scanning operation.
 * Holds references to the target project, session parameters, and boundary limits.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * </p>
 */
public class ScannerContext {

    private final ScanSession session;
    private final Project project;
    private final Path canonicalRoot;

    /**
     * Constructs a ScannerContext.
     *
     * @param session current scanning session
     * @param project target project aggregate
     */
    public ScannerContext(ScanSession session, Project project) {
        this.session = Objects.requireNonNull(session, "ScanSession must not be null");
        this.project = Objects.requireNonNull(project, "Project must not be null");
        this.canonicalRoot = Objects.requireNonNull(project.rootDirectory(), "Project root directory must not be null");
    }

    public ScanSession getSession() {
        return session;
    }

    public Project getProject() {
        return project;
    }

    public Path getCanonicalRoot() {
        return canonicalRoot;
    }
}
