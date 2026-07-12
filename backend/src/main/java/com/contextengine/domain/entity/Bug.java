package com.contextengine.domain.entity;

import com.contextengine.domain.valueobject.BugId;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import java.util.Objects;

/**
 * Tracks functional discrepancies, regression failures, or structural defects identified within the codebase workspace.
 */
public class Bug {
    
    private final BugId id;
    private final ProjectId projectId;
    private final Path filePath;
    private final int lineStart;
    private final int lineEnd;
    private final String commitHash;
    private BugState status;

    /**
     * Constructs a Bug in the UNRESOLVED state.
     * Enforces that a bug must be associated with at least one physical file path or a git commit hash.
     *
     * @param id the unique bug ID
     * @param projectId the parent project ID
     * @param filePath the file path where the bug manifests (nullable if commitHash is present)
     * @param lineStart starting line of defect (must be >= 1 if filePath is present)
     * @param lineEnd ending line of defect (must be >= lineStart if filePath is present)
     * @param commitHash the git commit hash linked to the defect (nullable if filePath is present)
     */
    public Bug(BugId id, ProjectId projectId, Path filePath, int lineStart, int lineEnd, String commitHash) {
        this.id = Objects.requireNonNull(id, "BugId must not be null");
        this.projectId = Objects.requireNonNull(projectId, "ProjectId must not be null");
        
        // Enforce validation: Must be associated with at least one physical file position or commit hash.
        if (filePath == null && (commitHash == null || commitHash.trim().isEmpty())) {
            throw new IllegalArgumentException("A Bug must be associated with at least one physical file path or a commit hash");
        }
        
        if (filePath != null) {
            if (lineStart < 1) {
                throw new IllegalArgumentException("Line start must be positive (>= 1)");
            }
            if (lineEnd < lineStart) {
                throw new IllegalArgumentException("Line end must be greater than or equal to line start");
            }
        }
        
        this.filePath = filePath;
        this.lineStart = lineStart;
        this.lineEnd = lineEnd;
        this.commitHash = commitHash == null ? "" : commitHash.trim();
        this.status = BugState.UNRESOLVED;
    }

    public BugId id() {
        return id;
    }

    public ProjectId projectId() {
        return projectId;
    }

    public Path filePath() {
        return filePath;
    }

    public int lineStart() {
        return lineStart;
    }

    public int lineEnd() {
        return lineEnd;
    }

    public String commitHash() {
        return commitHash;
    }

    public BugState status() {
        return status;
    }

    /**
     * Moves bug status to UNDER_INVESTIGATION.
     */
    public void startInvestigation() {
        if (this.status != BugState.UNRESOLVED) {
            throw new IllegalStateException("Bug must be UNRESOLVED to start investigation");
        }
        this.status = BugState.UNDER_INVESTIGATION;
    }

    /**
     * Resolves the bug.
     */
    public void resolve() {
        if (this.status != BugState.UNDER_INVESTIGATION) {
            throw new IllegalStateException("Bug must be UNDER_INVESTIGATION to resolve");
        }
        this.status = BugState.RESOLVED;
    }

    /**
     * Reopens a resolved bug.
     */
    public void reopen() {
        if (this.status != BugState.RESOLVED) {
            throw new IllegalStateException("Bug must be RESOLVED to reopen");
        }
        this.status = BugState.UNRESOLVED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bug bug)) return false;
        return id.equals(bug.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
