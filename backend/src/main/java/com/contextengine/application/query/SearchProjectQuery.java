package com.contextengine.application.query;

import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.SearchQuery;
import java.util.Objects;

/**
 * Query requesting to execute a search over a project's knowledge graph.
 */
public record SearchProjectQuery(
    ProjectId projectId,
    SearchQuery searchQuery
) implements Query {

    public SearchProjectQuery {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(searchQuery, "SearchQuery must not be null");
    }
}
