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
import com.contextengine.application.usecase.GetProjectUseCase;
import com.contextengine.application.usecase.ListProjectsUseCase;
import com.contextengine.application.usecase.RemoveProjectUseCase;
import com.contextengine.application.usecase.GetScanStatusUseCase;
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
import com.contextengine.application.scanner.dependency.ManifestParser;
import com.contextengine.application.scanner.dependency.DependencyScanner;
import com.contextengine.application.scanner.incremental.ChangeDetector;
import com.contextengine.application.scanner.validation.ScannerValidator;
import com.contextengine.application.knowledge.KnowledgeEngine;
import com.contextengine.application.knowledge.KnowledgeGraphBuilder;
import com.contextengine.application.knowledge.RelationshipResolver;
import com.contextengine.application.knowledge.retrieval.RetrievalEngine;
import com.contextengine.application.knowledge.ranking.RankingEngine;
import com.contextengine.application.knowledge.builder.ContextBuilder;
import com.contextengine.application.knowledge.budget.TokenBudgetManager;
import com.contextengine.application.knowledge.validation.ContextValidator;
import com.contextengine.application.knowledge.KnowledgeOptimizer;
import com.contextengine.persistence.repository.SpringDataKnowledgeNodeRepository;
import com.contextengine.persistence.repository.SpringDataKnowledgeRelationshipRepository;
import com.contextengine.infrastructure.metrics.TelemetryMetricsRegistry;
import com.contextengine.domain.event.DomainEventPublisher;
import com.contextengine.domain.repository.ContextRepository;
import com.contextengine.domain.repository.KnowledgeGraphRepository;
import com.contextengine.domain.repository.ProjectRepository;
import com.contextengine.domain.service.ContextGenerationService;
import com.contextengine.domain.service.ProjectRegistrationService;
import com.contextengine.domain.service.ProjectScannerService;
import com.contextengine.application.port.FilesystemPort;
import com.contextengine.application.port.GitPort;
import com.contextengine.domain.service.SearchService;
import com.contextengine.application.usecase.SearchUseCase;
import com.contextengine.mcp.protocol.McpProtocolHandler;
import com.contextengine.mcp.resource.McpResourceService;
import com.contextengine.mcp.prompt.McpPromptService;
import com.contextengine.mcp.session.McpSessionManager;
import com.contextengine.mcp.tool.McpTool;
import com.contextengine.mcp.tool.RegisterProjectTool;
import com.contextengine.mcp.tool.ScanProjectTool;
import com.contextengine.mcp.tool.GenerateContextTool;
import com.contextengine.mcp.tool.GenerateContextSnapshotTool;
import com.contextengine.mcp.tool.SearchTool;
import com.contextengine.mcp.tool.CoreSearchTool;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import java.util.List;
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
    public ManifestParser manifestParser() {
        return new ManifestParser();
    }

    @Bean
    public DependencyScanner dependencyScanner(FilesystemPort filesystemPort, ManifestParser manifestParser) {
        return new DependencyScanner(filesystemPort, manifestParser);
    }

    @Bean
    public ChangeDetector changeDetector() {
        return new ChangeDetector();
    }

    @Bean
    public ScannerValidator scannerValidator() {
        return new ScannerValidator();
    }

    @Bean
    public com.contextengine.application.knowledge.engine.KnowledgeEngine knowledgeEngineFoundation() {
        return new com.contextengine.application.knowledge.engine.KnowledgeEngineImpl();
    }

    @Bean
    public ScannerEngine scannerEngine(
        WorkspaceScanner workspaceScanner,
        DomainEventPublisher eventPublisher,
        ParserCoordinator parserCoordinator,
        SymbolExtractor symbolExtractor,
        FilesystemPort filesystemPort,
        ChangeDetector changeDetector,
        DependencyScanner dependencyScanner,
        ScannerValidator scannerValidator,
        com.contextengine.application.knowledge.engine.KnowledgeEngine knowledgeEngineFoundation
    ) {
        return new ScannerEngine(
            workspaceScanner,
            eventPublisher,
            parserCoordinator,
            symbolExtractor,
            filesystemPort,
            changeDetector,
            dependencyScanner,
            scannerValidator,
            knowledgeEngineFoundation
        );
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
        RetrievalEngine retrievalEngine,
        RankingEngine rankingEngine,
        ContextBuilder contextBuilder,
        TokenBudgetManager tokenBudgetManager,
        ContextValidator contextValidator,
        KnowledgeOptimizer knowledgeOptimizer,
        DomainEventPublisher eventPublisher
    ) {
        return new GenerateContextUseCase(
            contextRepository,
            retrievalEngine,
            rankingEngine,
            contextBuilder,
            tokenBudgetManager,
            contextValidator,
            knowledgeOptimizer,
            eventPublisher
        );
    }

    @Bean
    public GetLatestSnapshotUseCase getLatestSnapshotUseCase(ContextRepository contextRepository) {
        return new GetLatestSnapshotUseCase(contextRepository);
    }

    @Bean
    public GetProjectUseCase getProjectUseCase(ProjectRepository projectRepository) {
        return new GetProjectUseCase(projectRepository);
    }

    @Bean
    public ListProjectsUseCase listProjectsUseCase(ProjectRepository projectRepository) {
        return new ListProjectsUseCase(projectRepository);
    }

    @Bean
    public RemoveProjectUseCase removeProjectUseCase(ProjectRepository projectRepository) {
        return new RemoveProjectUseCase(projectRepository);
    }

    @Bean
    public GetScanStatusUseCase getScanStatusUseCase(ProjectRepository projectRepository) {
        return new GetScanStatusUseCase(projectRepository);
    }

    // Application Services

    @Bean
    public ProjectApplicationService projectApplicationService(
        RegisterProjectUseCase registerProjectUseCase,
        ScanProjectUseCase scanProjectUseCase,
        CreateFeatureUseCase createFeatureUseCase,
        CreateTaskUseCase createTaskUseCase,
        CreateDecisionUseCase createDecisionUseCase,
        GetProjectUseCase getProjectUseCase,
        ListProjectsUseCase listProjectsUseCase,
        RemoveProjectUseCase removeProjectUseCase,
        GetScanStatusUseCase getScanStatusUseCase,
        TransactionManager transactionManager,
        RegisterProjectCommandValidator registerProjectValidator
    ) {
        return new ProjectApplicationService(
            registerProjectUseCase,
            scanProjectUseCase,
            createFeatureUseCase,
            createTaskUseCase,
            createDecisionUseCase,
            getProjectUseCase,
            listProjectsUseCase,
            removeProjectUseCase,
            getScanStatusUseCase,
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

    // Knowledge Engine Beans

    @Bean
    public KnowledgeGraphBuilder knowledgeGraphBuilder() {
        return new KnowledgeGraphBuilder();
    }

    @Bean
    public RelationshipResolver relationshipResolver() {
        return new RelationshipResolver();
    }

    @Bean
    public KnowledgeEngine knowledgeEngine(
        KnowledgeGraphRepository graphRepository,
        KnowledgeGraphBuilder graphBuilder,
        RelationshipResolver relationshipResolver
    ) {
        return new KnowledgeEngine(graphRepository, graphBuilder, relationshipResolver);
    }

    @Bean
    public RetrievalEngine retrievalEngine(KnowledgeGraphRepository graphRepository) {
        return new RetrievalEngine(graphRepository);
    }

    @Bean
    public RankingEngine rankingEngine() {
        return new RankingEngine();
    }


    @Bean
    public TokenBudgetManager tokenBudgetManager(DomainEventPublisher eventPublisher) {
        return new TokenBudgetManager(eventPublisher);
    }

    @Bean
    public ContextValidator contextValidator() {
        return new ContextValidator();
    }

    @Bean
    public KnowledgeOptimizer knowledgeOptimizer(com.contextengine.infrastructure.cache.CacheManager cacheManager) {
        return new KnowledgeOptimizer(cacheManager);
    }

    @Bean
    public ContextBuilder contextBuilder() {
        return new ContextBuilder();
    }

    @Bean
    public SearchService searchService() {
        return new SearchService();
    }

    @Bean
    public SearchUseCase searchUseCase(SearchService searchService, KnowledgeGraphRepository graphRepository) {
        return new SearchUseCase(searchService, graphRepository);
    }

    @Bean
    public RegisterProjectTool registerProjectTool(ProjectApplicationService projectApplicationService) {
        return new RegisterProjectTool(projectApplicationService);
    }

    @Bean
    public ScanProjectTool scanProjectTool(ProjectApplicationService projectApplicationService) {
        return new ScanProjectTool(projectApplicationService);
    }

    @Bean
    public GenerateContextTool generateContextTool(ContextApplicationService contextApplicationService) {
        return new GenerateContextTool(contextApplicationService);
    }

    @Bean
    public GenerateContextSnapshotTool generateContextSnapshotTool(ContextApplicationService contextApplicationService) {
        return new GenerateContextSnapshotTool(contextApplicationService);
    }

    @Bean
    public SearchTool searchTool(SearchUseCase searchUseCase) {
        return new SearchTool(searchUseCase);
    }

    @Bean
    public CoreSearchTool coreSearchTool(SearchUseCase searchUseCase) {
        return new CoreSearchTool(searchUseCase);
    }

    @Bean
    public McpResourceService mcpResourceService(
        ProjectRepository projectRepository,
        ContextRepository contextRepository,
        KnowledgeGraphRepository graphRepository
    ) {
        return new McpResourceService(projectRepository, contextRepository, graphRepository);
    }

    @Bean
    public McpPromptService mcpPromptService(ProjectRepository projectRepository) {
        return new McpPromptService(projectRepository);
    }

    @Bean
    public McpSessionManager mcpSessionManager() {
        return new McpSessionManager();
    }

    @Bean
    public McpProtocolHandler mcpProtocolHandler(
        List<McpTool> tools,
        McpResourceService resourceService,
        McpPromptService promptService,
        McpSessionManager sessionManager,
        com.contextengine.mcp.security.McpAuthenticationService authenticationService,
        com.contextengine.mcp.security.McpAuthorizationService authorizationService,
        com.contextengine.mcp.security.McpScopeValidator scopeValidator,
        ProjectRepository projectRepository,
        com.contextengine.security.authorization.AuthorizationService coreAuthorizationService,
        TelemetryMetricsRegistry metricsRegistry
    ) {
        return new McpProtocolHandler(
            tools,
            resourceService,
            promptService,
            sessionManager,
            authenticationService,
            authorizationService,
            scopeValidator,
            projectRepository,
            coreAuthorizationService,
            metricsRegistry
        );
    }

    @Bean
    public com.contextengine.security.authentication.CredentialValidator credentialValidator(
        com.contextengine.configuration.properties.McpProperties mcpProperties
    ) {
        return new com.contextengine.security.authentication.CredentialValidator(mcpProperties.securityTokens());
    }

    @Bean
    public com.contextengine.security.authentication.AuthenticationService authenticationService(
        com.contextengine.security.authentication.CredentialValidator credentialValidator
    ) {
        return new com.contextengine.security.authentication.AuthenticationService(credentialValidator);
    }

    @Bean
    public com.contextengine.security.authorization.AuthorizationService authorizationService() {
        return new com.contextengine.security.authorization.AuthorizationService();
    }

    @Bean
    public com.contextengine.security.isolation.WorkspaceBoundaryValidator workspaceBoundaryValidator() {
        return new com.contextengine.security.isolation.WorkspaceBoundaryValidator();
    }

    @Bean
    public com.contextengine.security.isolation.WorkspaceIsolationService workspaceIsolationService(
        ProjectRepository projectRepository,
        com.contextengine.security.isolation.WorkspaceBoundaryValidator workspaceBoundaryValidator
    ) {
        return new com.contextengine.security.isolation.WorkspaceIsolationService(projectRepository, workspaceBoundaryValidator);
    }

    @Bean
    public com.contextengine.security.validation.SecurityInputValidator securityInputValidator() {
        return new com.contextengine.security.validation.SecurityInputValidator();
    }

    @Bean
    public com.contextengine.security.filesystem.SecureFilesystemService secureFilesystemService(
        FilesystemPort filesystemPort,
        com.contextengine.security.isolation.WorkspaceIsolationService workspaceIsolationService
    ) {
        return new com.contextengine.security.filesystem.SecureFilesystemService(filesystemPort, workspaceIsolationService);
    }
}
