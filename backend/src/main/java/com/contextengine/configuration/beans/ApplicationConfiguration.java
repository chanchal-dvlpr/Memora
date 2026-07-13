package com.contextengine.configuration.beans;

import com.contextengine.application.port.TransactionManager;
import com.contextengine.application.service.ContextApplicationService;
import com.contextengine.application.service.ProjectApplicationService;
import com.contextengine.application.usecase.RegisterProjectUseCase;
import com.contextengine.application.usecase.ScanProjectUseCase;
import com.contextengine.application.usecase.CreateFeatureUseCase;
import com.contextengine.application.usecase.CreateTaskUseCase;
import com.contextengine.application.usecase.CreateDecisionUseCase;
import com.contextengine.application.usecase.GenerateContextUseCase;
import com.contextengine.application.usecase.GetLatestSnapshotUseCase;
import com.contextengine.application.validation.GenerateContextCommandValidator;
import com.contextengine.application.validation.RegisterProjectCommandValidator;
import com.contextengine.application.scanner.WorkspaceTraversalService;
import com.contextengine.application.scanner.FileFilter;
import com.contextengine.application.scanner.FileDiscoveryService;
import com.contextengine.application.scanner.WorkspaceScanner;
import com.contextengine.application.scanner.ScannerEngine;
import com.contextengine.application.scanner.LanguageDetector;
import com.contextengine.application.scanner.ParserRegistryBroker;
import com.contextengine.application.scanner.ParserRegistry;
import com.contextengine.application.scanner.ParserCoordinator;
import com.contextengine.application.scanner.SymbolExtractor;
import com.contextengine.domain.event.DomainEventPublisher;
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

    // Scanner Framework Beans

    @Bean
    public WorkspaceTraversalService workspaceTraversalService(FilesystemPort filesystemPort) {
        return new WorkspaceTraversalService(filesystemPort);
    }

    @Bean
    public FileFilter fileFilter() {
        return new FileFilter();
    }

    @Bean
    public LanguageDetector languageDetector() {
        return new LanguageDetector();
    }

    @Bean
    public ParserRegistryBroker parserRegistryBroker(com.contextengine.infrastructure.parser.ILanguageParserFactory languageParserFactory) {
        return new ParserRegistry(languageParserFactory);
    }

    @Bean
    public ParserCoordinator parserCoordinator(ParserRegistryBroker parserRegistryBroker) {
        return new ParserCoordinator(parserRegistryBroker);
    }

    @Bean
    public SymbolExtractor symbolExtractor() {
        return new SymbolExtractor();
    }

    @Bean
    public FileDiscoveryService fileDiscoveryService(
        WorkspaceTraversalService traversalService,
        FileFilter fileFilter,
        LanguageDetector languageDetector
    ) {
        return new FileDiscoveryService(traversalService, fileFilter, languageDetector);
    }

    @Bean
    public WorkspaceScanner workspaceScanner(FileDiscoveryService fileDiscoveryService) {
        return new WorkspaceScanner(fileDiscoveryService);
    }

    @Bean
    public ScannerEngine scannerEngine(
        WorkspaceScanner workspaceScanner,
        DomainEventPublisher eventPublisher,
        ParserCoordinator parserCoordinator,
        SymbolExtractor symbolExtractor,
        FilesystemPort filesystemPort
    ) {
        return new ScannerEngine(workspaceScanner, eventPublisher, parserCoordinator, symbolExtractor, filesystemPort);
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
        ScannerEngine scannerEngine
    ) {
        return new ScanProjectUseCase(projectRepository, filesystemPort, gitPort, scannerEngine);
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
