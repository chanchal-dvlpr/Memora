package com.contextengine.application.service;

import com.contextengine.application.command.*;
import com.contextengine.application.dto.*;
import com.contextengine.application.port.TransactionManager;
import com.contextengine.application.result.ApplicationResult;
import com.contextengine.application.usecase.*;
import com.contextengine.application.validation.RegisterProjectCommandValidator;
import java.util.Objects;

/**
 * Application service coordinating project registration, workspace scanning, and management.
 * Enforces transaction boundaries and request validation before delegating to use cases.
 * <p>
 * Bounded Context: Project Management
 * Related Command / Query: RegisterProjectCommand, ScanProjectCommand, CreateFeatureCommand, CreateTaskCommand, CreateDecisionCommand
 * Related Aggregate: Project
 * </p>
 */
public class ProjectApplicationService {

    private final RegisterProjectUseCase registerProjectUseCase;
    private final ScanProjectUseCase scanProjectUseCase;
    private final CreateFeatureUseCase createFeatureUseCase;
    private final CreateTaskUseCase createTaskUseCase;
    private final CreateDecisionUseCase createDecisionUseCase;
    private final TransactionManager transactionManager;
    private final RegisterProjectCommandValidator registerProjectValidator;

    /**
     * Constructs a ProjectApplicationService.
     *
     * @param registerProjectUseCase use case
     * @param scanProjectUseCase use case
     * @param createFeatureUseCase use case
     * @param createTaskUseCase use case
     * @param createDecisionUseCase use case
     * @param transactionManager transaction manager port
     * @param registerProjectValidator validator component
     */
    public ProjectApplicationService(
        RegisterProjectUseCase registerProjectUseCase,
        ScanProjectUseCase scanProjectUseCase,
        CreateFeatureUseCase createFeatureUseCase,
        CreateTaskUseCase createTaskUseCase,
        CreateDecisionUseCase createDecisionUseCase,
        TransactionManager transactionManager,
        RegisterProjectCommandValidator registerProjectValidator
    ) {
        this.registerProjectUseCase = Objects.requireNonNull(registerProjectUseCase, "RegisterProjectUseCase must not be null");
        this.scanProjectUseCase = Objects.requireNonNull(scanProjectUseCase, "ScanProjectUseCase must not be null");
        this.createFeatureUseCase = Objects.requireNonNull(createFeatureUseCase, "CreateFeatureUseCase must not be null");
        this.createTaskUseCase = Objects.requireNonNull(createTaskUseCase, "CreateTaskUseCase must not be null");
        this.createDecisionUseCase = Objects.requireNonNull(createDecisionUseCase, "CreateDecisionUseCase must not be null");
        this.transactionManager = Objects.requireNonNull(transactionManager, "TransactionManager must not be null");
        this.registerProjectValidator = Objects.requireNonNull(registerProjectValidator, "RegisterProjectCommandValidator must not be null");
    }

    /**
     * Traverses filesystem validation and initiates a new project registration transaction.
     *
     * @param command the registration request
     * @return result containing the created Project DTO
     */
    public ApplicationResult<ProjectDto> registerProject(RegisterProjectCommand command) {
        try {
            registerProjectValidator.validate(command);
            return transactionManager.executeInTransaction(() -> registerProjectUseCase.execute(command));
        } catch (Exception e) {
            return ApplicationResult.failure(new com.contextengine.application.exception.ApplicationException("Validation failed for project registration", e));
        }
    }

    /**
     * Enforces repository locks and indexes active relative workspace files recursively.
     *
     * @param command the workspace scan command
     * @return result containing success state boolean
     */
    public ApplicationResult<Boolean> scanProject(ScanProjectCommand command) {
        return transactionManager.executeInTransaction(() -> scanProjectUseCase.execute(command));
    }

    /**
     * Registers a new project roadmapped feature.
     *
     * @param command the feature parameters command
     * @return result containing the created Feature DTO
     */
    public ApplicationResult<FeatureDto> createFeature(CreateFeatureCommand command) {
        return transactionManager.executeInTransaction(() -> createFeatureUseCase.execute(command));
    }

    /**
     * Appends a granular development task to the project.
     *
     * @param command the task parameters command
     * @return result containing the created Task DTO
     */
    public ApplicationResult<TaskDto> createTask(CreateTaskCommand command) {
        return transactionManager.executeInTransaction(() -> createTaskUseCase.execute(command));
    }

    /**
     * Registers an architectural decision (ADR).
     *
     * @param command the decision parameters command
     * @return result containing the created Decision DTO
     */
    public ApplicationResult<DecisionDto> createDecision(CreateDecisionCommand command) {
        return transactionManager.executeInTransaction(() -> createDecisionUseCase.execute(command));
    }
}
