package com.contextengine.configuration.beans;

import com.contextengine.application.port.TransactionManager;
import com.contextengine.application.service.ContextApplicationService;
import com.contextengine.application.service.ProjectApplicationService;
import com.contextengine.application.usecase.*;
import com.contextengine.application.validation.GenerateContextCommandValidator;
import com.contextengine.application.validation.RegisterProjectCommandValidator;
import com.contextengine.domain.repository.ContextRepository;
import com.contextengine.domain.repository.KnowledgeGraphRepository;
import com.contextengine.domain.repository.ProjectRepository;
import com.contextengine.domain.service.ContextGenerationService;
import com.contextengine.domain.service.ProjectRegistrationService;
import com.contextengine.domain.service.ProjectScannerService;
import com.contextengine.application.port.FilesystemPort;
import com.contextengine.application.port.GitPort;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables shared application infrastructure and registers application layer beans.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
@EnableConfigurationProperties
@ConfigurationPropertiesScan(basePackages = "com.contextengine.configuration.properties")
public class ApplicationConfiguration {

    // Domain Services

    @Bean
    public ProjectRegistrationService projectRegistrationService() {
        return new ProjectRegistrationService();
    }

    @Bean
    public ProjectScannerService projectScannerService() {
        return new ProjectScannerService();
    }

    @Bean
    public ContextGenerationService contextGenerationService() {
        return new ContextGenerationService();
    }

    // Validators

    @Bean
    public RegisterProjectCommandValidator registerProjectCommandValidator() {
        return new RegisterProjectCommandValidator();
    }

    @Bean
    public GenerateContextCommandValidator generateContextCommandValidator() {
        return new GenerateContextCommandValidator();
    }

    // Use Cases

    @Bean
    public RegisterProjectUseCase registerProjectUseCase(
        ProjectRepository projectRepository,
        FilesystemPort filesystemPort,
        ProjectRegistrationService registrationService
    ) {
        return new RegisterProjectUseCase(projectRepository, filesystemPort, registrationService);
    }

    @Bean
    public ScanProjectUseCase scanProjectUseCase(
        ProjectRepository projectRepository,
        FilesystemPort filesystemPort,
        GitPort gitPort,
        ProjectScannerService scannerService
    ) {
        return new ScanProjectUseCase(projectRepository, filesystemPort, gitPort, scannerService);
    }

    @Bean
    public CreateFeatureUseCase createFeatureUseCase(ProjectRepository projectRepository) {
        return new CreateFeatureUseCase(projectRepository);
    }

    @Bean
    public CreateTaskUseCase createTaskUseCase(ProjectRepository projectRepository) {
        return new CreateTaskUseCase(projectRepository);
    }

    @Bean
    public CreateDecisionUseCase createDecisionUseCase(ProjectRepository projectRepository) {
        return new CreateDecisionUseCase(projectRepository);
    }

    @Bean
    public GenerateContextUseCase generateContextUseCase(
        ContextRepository contextRepository,
        KnowledgeGraphRepository graphRepository,
        ContextGenerationService generationService
    ) {
        return new GenerateContextUseCase(contextRepository, graphRepository, generationService);
    }

    @Bean
    public GetLatestSnapshotUseCase getLatestSnapshotUseCase(ContextRepository contextRepository) {
        return new GetLatestSnapshotUseCase(contextRepository);
    }

    // Application Services

    @Bean
    public ProjectApplicationService projectApplicationService(
        RegisterProjectUseCase registerProjectUseCase,
        ScanProjectUseCase scanProjectUseCase,
        CreateFeatureUseCase createFeatureUseCase,
        CreateTaskUseCase createTaskUseCase,
        CreateDecisionUseCase createDecisionUseCase,
        TransactionManager transactionManager,
        RegisterProjectCommandValidator registerProjectValidator
    ) {
        return new ProjectApplicationService(
            registerProjectUseCase,
            scanProjectUseCase,
            createFeatureUseCase,
            createTaskUseCase,
            createDecisionUseCase,
            transactionManager,
            registerProjectValidator
        );
    }

    @Bean
    public ContextApplicationService contextApplicationService(
        GenerateContextUseCase generateContextUseCase,
        GetLatestSnapshotUseCase getLatestSnapshotUseCase,
        TransactionManager transactionManager,
        GenerateContextCommandValidator generateContextValidator
    ) {
        return new ContextApplicationService(
            generateContextUseCase,
            getLatestSnapshotUseCase,
            transactionManager,
            generateContextValidator
        );
    }
}
