package com.contextengine.application.port;

import java.util.function.Supplier;

/**
 * Port interface defining transaction management boundaries within the Application Layer.
 * Allows the orchestrator to wrap use case operations inside atomic execution contexts.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Database Persistence
 * </p>
 */
public interface TransactionManager {
    
    /**
     * Executes the given callback operation within an active transactional boundary.
     * Rollbacks state changes if an exception is thrown.
     *
     * @param callback the operational callback to run
     * @param <T> the type of execution result
     * @return the result returned by the callback
     */
    <T> T executeInTransaction(Supplier<T> callback);
}
