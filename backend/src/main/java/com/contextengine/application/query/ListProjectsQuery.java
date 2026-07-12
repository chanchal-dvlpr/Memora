package com.contextengine.application.query;

/**
 * Query requesting to retrieve all registered projects currently active in the workspace index registry.
 * <p>
 * Related Bounded Context / Aggregate: Project
 * Related Repository: ProjectRepository
 * </p>
 */
public record ListProjectsQuery(
    boolean activeOnly
) implements Query {
}
