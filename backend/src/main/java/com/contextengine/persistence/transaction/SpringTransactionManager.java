package com.contextengine.persistence.transaction;

import com.contextengine.application.port.TransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Spring-based implementation of the Application TransactionManager port.
 * Allows programmatic execution of use cases within atomic transaction boundaries.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Database Persistence
 * </p>
 */
public class SpringTransactionManager implements TransactionManager {

    private final TransactionTemplate transactionTemplate;

    /**
     * Constructs a SpringTransactionManager.
     *
     * @param transactionTemplate Spring programmatic transaction helper
     */
    public SpringTransactionManager(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "TransactionTemplate must not be null");
    }

    @Override
    public <T> T executeInTransaction(Supplier<T> callback) {
        Objects.requireNonNull(callback, "Callback must not be null");
        return transactionTemplate.execute(status -> callback.get());
    }
}
