package com.contextengine.application.service;

import com.contextengine.application.command.GenerateContextCommand;
import com.contextengine.application.dto.ContextSnapshotDto;
import com.contextengine.application.port.TransactionManager;
import com.contextengine.application.query.GetLatestSnapshotQuery;
import com.contextengine.application.result.ApplicationResult;
import com.contextengine.application.usecase.*;
import com.contextengine.application.validation.GenerateContextCommandValidator;
import java.util.Objects;

/**
 * Application service coordinating context generation, token budget enforcement, and snapshot retrieval.
 * Enforces transaction boundaries and request validation before delegating to use cases.
 * <p>
 * Bounded Context: Context Assembly
 * Related Command / Query: GenerateContextCommand, GetLatestSnapshotQuery
 * Related Aggregate: Context / ContextSnapshot
 * </p>
 */
public class ContextApplicationService {

    private final GenerateContextUseCase generateContextUseCase;
    private final GetLatestSnapshotUseCase getLatestSnapshotUseCase;
    private final TransactionManager transactionManager;
    private final GenerateContextCommandValidator generateContextValidator;

    /**
     * Constructs a ContextApplicationService.
     *
     * @param generateContextUseCase use case
     * @param getLatestSnapshotUseCase use case
     * @param transactionManager transaction manager port
     * @param generateContextValidator validator component
     */
    public ContextApplicationService(
        GenerateContextUseCase generateContextUseCase,
        GetLatestSnapshotUseCase getLatestSnapshotUseCase,
        TransactionManager transactionManager,
        GenerateContextCommandValidator generateContextValidator
    ) {
        this.generateContextUseCase = Objects.requireNonNull(generateContextUseCase, "GenerateContextUseCase must not be null");
        this.getLatestSnapshotUseCase = Objects.requireNonNull(getLatestSnapshotUseCase, "GetLatestSnapshotUseCase must not be null");
        this.transactionManager = Objects.requireNonNull(transactionManager, "TransactionManager must not be null");
        this.generateContextValidator = Objects.requireNonNull(generateContextValidator, "GenerateContextCommandValidator must not be null");
    }

    /**
     * Generates a context snapshot and persists it within an active transaction boundary.
     *
     * @param command the context generation command
     * @return result containing the generated ContextSnapshot DTO
     */
    public ApplicationResult<ContextSnapshotDto> generateContext(GenerateContextCommand command) {
        try {
            generateContextValidator.validate(command);
            return transactionManager.executeInTransaction(() -> generateContextUseCase.execute(command));
        } catch (Exception e) {
            return ApplicationResult.failure(new com.contextengine.application.exception.ApplicationException("Validation failed for context generation", e));
        }
    }

    /**
     * Queries the latest generated context snapshot for a project.
     *
     * @param query the query containing project ID
     * @return result containing the latest snapshot DTO
     */
    public ApplicationResult<ContextSnapshotDto> getLatestSnapshot(GetLatestSnapshotQuery query) {
        return getLatestSnapshotUseCase.execute(query);
    }
}
