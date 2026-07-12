package com.contextengine.application.query;

/**
 * Handles read-only Queries and returns results.
 *
 * @param <Q> the specific Query type
 * @param <R> the output type
 */
public interface QueryHandler<Q extends Query, R> {
    
    /**
     * Handles query execution.
     *
     * @param query the query payload
     * @return the queried model results
     */
    R handle(Q query);
}
