package com.contextengine.application.knowledge.retrieval;

import com.contextengine.domain.valueobject.ProjectId;
import java.util.Objects;

/**
 * Encapsulates the parameters for a knowledge retrieval request.
 * <p>
 * Bounded Context: Knowledge Graph / Context Assembly
 * Architecture Reference: Functional Requirement FR-014 (Context Generation Engine) Section 6.1
 * Responsibility: Holder of input parameters for the Retrieval Engine.
 * Dependencies: {@link ProjectId}.
 * Future Usage: Will support token filtering and multi-view configurations in future phases.
 * </p>
 */
public record RetrievalQuery(
    ProjectId projectId,
    String contextQuery,
    String activeFilePath,
    int depthLimit
) {
    /**
     * Constructs a RetrievalQuery.
     *
     * @param projectId the associated project ID
     * @param contextQuery semantic search query text
     * @param activeFilePath optional path to the active file being edited
     * @param depthLimit maximum traversal depth limit
     * @throws NullPointerException if projectId or contextQuery is null
     * @throws IllegalArgumentException if depthLimit is negative
     */
    public RetrievalQuery {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(contextQuery, "ContextQuery must not be null");
        if (depthLimit < 0) {
            throw new IllegalArgumentException("Depth limit must be non-negative");
        }
    }
}
