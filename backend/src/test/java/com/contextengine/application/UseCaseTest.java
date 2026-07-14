package com.contextengine.application;

import com.contextengine.application.command.*;
import com.contextengine.application.dto.*;
import com.contextengine.application.port.*;
import com.contextengine.application.query.GetLatestSnapshotQuery;
import com.contextengine.application.result.ApplicationResult;
import com.contextengine.application.usecase.*;
import com.contextengine.domain.entity.*;
import com.contextengine.domain.repository.ContextRepository;
import com.contextengine.domain.repository.KnowledgeGraphRepository;
import com.contextengine.domain.repository.ProjectRepository;
import com.contextengine.domain.service.ContextGenerationService;
import com.contextengine.domain.service.FormatEnum;
import com.contextengine.domain.service.ProjectRegistrationService;
import com.contextengine.domain.service.ProjectScannerService;
import com.contextengine.domain.valueobject.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.contextengine.test.BaseUnitTest;
import com.contextengine.test.TestDataFactory;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class UseCaseTest extends BaseUnitTest {

    private ProjectRepository projectRepository;
    private ContextRepository contextRepository;
    private KnowledgeGraphRepository graphRepository;
    
    private FilesystemPort filesystemPort;
    private GitPort gitPort;
    
    private ProjectRegistrationService registrationService;
    private ProjectScannerService scannerService;
    private ContextGenerationService generationService;

    private TransactionManager transactionManager;
    private com.contextengine.application.validation.RegisterProjectCommandValidator registerProjectValidator;
    private com.contextengine.application.validation.GenerateContextCommandValidator generateContextValidator;
    private com.contextengine.application.service.ProjectApplicationService projectApplicationService;
    private com.contextengine.application.service.ContextApplicationService contextApplicationService;
    private com.contextengine.application.knowledge.retrieval.RetrievalEngine retrievalEngine;
    private com.contextengine.application.knowledge.ranking.RankingEngine rankingEngine;
    private com.contextengine.application.knowledge.builder.ContextBuilder contextBuilder;
    private com.contextengine.application.knowledge.budget.TokenBudgetManager tokenBudgetManager;
    private com.contextengine.application.knowledge.validation.ContextValidator contextValidator;
    private com.contextengine.application.knowledge.KnowledgeOptimizer knowledgeOptimizer;
    private com.contextengine.domain.event.DomainEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        projectRepository = new ProjectRepository() {
            private final Map<ProjectId, Project> store = new HashMap<>();

            @Override
            public void save(Project project) {
                store.put(project.id(), project);
            }

            @Override
            public Optional<Project> findById(ProjectId id) {
                return Optional.ofNullable(store.get(id));
            }

            @Override
            public Optional<Project> findByPath(Path absolutePath) {
                return store.values().stream().filter(p -> p.rootDirectory().equals(absolutePath)).findFirst();
            }

            @Override
            public Collection<Project> findAllActive() {
                return new ArrayList<>(store.values());
            }

            @Override
            public void remove(ProjectId id) {
                store.remove(id);
            }
        };

        contextRepository = new ContextRepository() {
            private final Map<ProjectId, Context> store = new HashMap<>();

            @Override
            public void save(Context context) {
                store.put(context.projectId(), context);
            }

            @Override
            public Optional<ContextSnapshot> findSnapshotById(SnapshotId snapshotId) {
                return store.values().stream()
                    .flatMap(c -> c.snapshots().stream())
                    .filter(s -> s.id().equals(snapshotId))
                    .findFirst();
            }

            @Override
            public Optional<ContextSnapshot> findLatestSnapshotForProject(ProjectId projectId) {
                Context context = store.get(projectId);
                if (context == null || context.snapshots().isEmpty()) {
                    return Optional.empty();
                }
                return Optional.of(context.snapshots().get(context.snapshots().size() - 1));
            }

            @Override
            public Collection<ContextSnapshot> findHistoryByDateRange(ProjectId projectId, DateRange range) {
                Context context = store.get(projectId);
                if (context == null) return List.of();
                return context.snapshots();
            }

            @Override
            public int pruneOldSnapshots(ProjectId projectId, Timestamp retentionCutoff) {
                return 0;
            }

            @Override
            public void removeSnapshot(SnapshotId snapshotId) {
            }
        };

        graphRepository = new KnowledgeGraphRepository() {
            private final List<KnowledgeNode> nodes = new ArrayList<>();

            @Override
            public void save(KnowledgeGraph graph) {
            }

            @Override
            public void commit(GraphTransaction transaction) {
            }

            @Override
            public Optional<KnowledgeNode> findNodeById(NodeId id) {
                return nodes.stream().filter(n -> n.id().equals(id)).findFirst();
            }

            @Override
            public Optional<KnowledgeRelationship> findRelationshipById(RelationshipId relationshipId) {
                return Optional.empty();
            }

            @Override
            public Collection<KnowledgeRelationship> findEdgesFrom(NodeId sourceNodeId) {
                return List.of();
            }

            @Override
            public Collection<KnowledgeRelationship> findEdgesTo(NodeId targetNodeId) {
                return List.of();
            }

            @Override
            public Collection<KnowledgeRelationship> findEdges(NodeId nodeId, Direction direction) {
                return List.of();
            }

            @Override
            public Collection<KnowledgeNode> querySubGraph(NodeId centerNodeId, int depthMax) {
                return List.of(new KnowledgeNode(centerNodeId, "FILE", new Metadata(Map.of("tokens", "100"))));
            }

            @Override
            public void removeNode(NodeId nodeId) {
            }

            @Override
            public Collection<KnowledgeNode> findNodesByProject(ProjectId projectId) {
                return List.of(new KnowledgeNode(NodeId.generate(), "FILE", new Metadata(Map.of("tokens", "100", "urn", "urn:ce:node:test:file:app", "qualifiedName", "App"))));
            }

            @Override
            public Collection<KnowledgeRelationship> findRelationshipsByProject(ProjectId projectId) {
                return List.of();
            }
        };

        // In-memory Stubs for Ports
        filesystemPort = new FilesystemPort() {
            @Override
            public boolean exists(Path path) {
                return path.value().equals(System.getProperty("user.dir")) || "/valid/path".equals(path.value());
            }

            @Override
            public boolean isDirectory(Path path) {
                return true;
            }

            @Override
            public boolean hasReadWritePermissions(Path path) {
                return path.value().equals(System.getProperty("user.dir")) || "/valid/path".equals(path.value());
            }

            @Override
            public List<Path> listFiles(Path root, List<String> exclusions) {
                return List.of(new Path("src/App.java"));
            }

            @Override
            public String readFile(Path filePath) {
                return "class App {}";
            }
        };

        gitPort = new GitPort() {
            @Override
            public boolean isGitRepository(Path directory) {
                return true;
            }

            @Override
            public String getActiveBranch(Path directory) {
                return "main";
            }

            @Override
            public String getLatestCommitHash(Path directory) {
                return "f00b4rhash";
            }
        };

        // Domain Services
        registrationService = new ProjectRegistrationService();
        scannerService = new ProjectScannerService();
        generationService = new ContextGenerationService();

        // Transaction Manager and Validators
        transactionManager = new TransactionManager() {
            @Override
            public <T> T executeInTransaction(java.util.function.Supplier<T> callback) {
                return callback.get();
            }
        };

        registerProjectValidator = new com.contextengine.application.validation.RegisterProjectCommandValidator();
        generateContextValidator = new com.contextengine.application.validation.GenerateContextCommandValidator();

        // Scanner components for ScanProjectUseCase
        com.contextengine.application.scanner.WorkspaceTraversalService traversalService =
            new com.contextengine.application.scanner.WorkspaceTraversalService(filesystemPort);
        com.contextengine.application.scanner.LanguageDetector languageDetector =
            new com.contextengine.application.scanner.LanguageDetector();
        com.contextengine.application.scanner.FileFilter fileFilter = new com.contextengine.application.scanner.FileFilter();
        com.contextengine.application.scanner.FileDiscoveryService fileDiscoveryService =
            new com.contextengine.application.scanner.FileDiscoveryService(traversalService, fileFilter, languageDetector);
        com.contextengine.application.scanner.WorkspaceScanner workspaceScanner =
            new com.contextengine.application.scanner.WorkspaceScanner(fileDiscoveryService);
        this.eventPublisher = event -> { /* no-op */ };

        com.contextengine.infrastructure.parser.LanguageParserFactory parserFactory =
            new com.contextengine.infrastructure.parser.LanguageParserFactory();
        com.contextengine.application.scanner.ParserRegistry registry =
            new com.contextengine.application.scanner.ParserRegistry(parserFactory);
        com.contextengine.application.scanner.ParserCoordinator parserCoordinator =
            new com.contextengine.application.scanner.ParserCoordinator(registry);
        com.contextengine.application.scanner.SymbolExtractor symbolExtractor =
            new com.contextengine.application.scanner.SymbolExtractor();

        com.contextengine.application.scanner.incremental.ChangeDetector changeDetector =
            new com.contextengine.application.scanner.incremental.ChangeDetector();
        com.contextengine.application.scanner.dependency.ManifestParser manifestParser =
            new com.contextengine.application.scanner.dependency.ManifestParser();
        com.contextengine.application.scanner.dependency.DependencyScanner dependencyScanner =
            new com.contextengine.application.scanner.dependency.DependencyScanner(filesystemPort, manifestParser);
        com.contextengine.application.scanner.validation.ScannerValidator scannerValidator =
            new com.contextengine.application.scanner.validation.ScannerValidator();

        com.contextengine.application.scanner.ScannerEngine scannerEngine =
            new com.contextengine.application.scanner.ScannerEngine(
                workspaceScanner,
                eventPublisher,
                parserCoordinator,
                symbolExtractor,
                filesystemPort,
                changeDetector,
                dependencyScanner,
                scannerValidator,
                new com.contextengine.application.knowledge.engine.KnowledgeEngineImpl()
            );

        projectApplicationService = new com.contextengine.application.service.ProjectApplicationService(
            new RegisterProjectUseCase(projectRepository, filesystemPort, registrationService),
            new ScanProjectUseCase(projectRepository, filesystemPort, gitPort, scannerEngine),
            new CreateFeatureUseCase(projectRepository),
            new CreateTaskUseCase(projectRepository),
            new CreateDecisionUseCase(projectRepository),
            new com.contextengine.application.usecase.GetProjectUseCase(projectRepository),
            new com.contextengine.application.usecase.ListProjectsUseCase(projectRepository),
            new com.contextengine.application.usecase.RemoveProjectUseCase(projectRepository),
            new com.contextengine.application.usecase.GetScanStatusUseCase(projectRepository),
            transactionManager,
            registerProjectValidator
        );

        com.contextengine.persistence.repository.SpringDataKnowledgeNodeRepository nodeRepository =
            org.mockito.Mockito.mock(com.contextengine.persistence.repository.SpringDataKnowledgeNodeRepository.class);
        com.contextengine.persistence.repository.SpringDataKnowledgeRelationshipRepository relationshipRepository =
            org.mockito.Mockito.mock(com.contextengine.persistence.repository.SpringDataKnowledgeRelationshipRepository.class);

        com.contextengine.persistence.entity.KnowledgeNodeEntity nodeEntity = new com.contextengine.persistence.entity.KnowledgeNodeEntity();
        nodeEntity.setId(java.util.UUID.randomUUID().toString());
        nodeEntity.setSymbolType("FILE");
        nodeEntity.setQualifiedName("App");
        nodeEntity.setNodeProperties("tokens:100,urn:urn:ce:node:test:file:app");

        org.mockito.Mockito.lenient().when(nodeRepository.findByProjectId(org.mockito.Mockito.anyString()))
            .thenReturn(java.util.List.of(nodeEntity));
        org.mockito.Mockito.lenient().when(relationshipRepository.findByProjectId(org.mockito.Mockito.anyString()))
            .thenReturn(java.util.List.of());

        retrievalEngine = new com.contextengine.application.knowledge.retrieval.RetrievalEngine(graphRepository);
        rankingEngine = new com.contextengine.application.knowledge.ranking.RankingEngine();
        contextBuilder = new com.contextengine.application.knowledge.builder.ContextBuilder();
        this.tokenBudgetManager =
            new com.contextengine.application.knowledge.budget.TokenBudgetManager(eventPublisher);
        this.contextValidator =
            new com.contextengine.application.knowledge.validation.ContextValidator();
        com.contextengine.infrastructure.cache.CacheManager cacheManager =
            new com.contextengine.infrastructure.cache.CacheManager();
        this.knowledgeOptimizer =
            new com.contextengine.application.knowledge.KnowledgeOptimizer(cacheManager);

        contextApplicationService = new com.contextengine.application.service.ContextApplicationService(
            new GenerateContextUseCase(
                contextRepository,
                retrievalEngine,
                rankingEngine,
                contextBuilder,
                tokenBudgetManager,
                contextValidator,
                knowledgeOptimizer,
                eventPublisher
            ),
            new GetLatestSnapshotUseCase(contextRepository),
            transactionManager,
            generateContextValidator
        );
    }

    @Test
    void testRegisterProjectUseCaseSuccess() {
        RegisterProjectUseCase useCase = new RegisterProjectUseCase(projectRepository, filesystemPort, registrationService);
        RegisterProjectCommand cmd = new RegisterProjectCommand(new Path(System.getProperty("user.dir")), "Test Proj", List.of());

        ApplicationResult<ProjectDto> result = useCase.execute(cmd);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().get().title()).isEqualTo("Test Proj");
        assertThat(result.value().get().absoluteRootPath()).isEqualTo(System.getProperty("user.dir"));
    }

    @Test
    void testRegisterProjectUseCaseDirectoryNotExists() {
        RegisterProjectUseCase useCase = new RegisterProjectUseCase(projectRepository, filesystemPort, registrationService);
        RegisterProjectCommand cmd = new RegisterProjectCommand(new Path("/invalid/path"), "Test Proj", List.of());

        ApplicationResult<ProjectDto> result = useCase.execute(cmd);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().get().getMessage()).contains("Project directory does not exist");
    }

    @Test
    void testScanProjectUseCaseSuccess() {
        Project project = registrationService.registerProject(new Path(System.getProperty("user.dir")), "Test Proj", List.of());
        Workspace workspace = new Workspace(WorkspaceId.generate(), project.id());
        project.bindWorkspace(workspace);
        projectRepository.save(project);

        com.contextengine.application.scanner.WorkspaceTraversalService traversalService =
            new com.contextengine.application.scanner.WorkspaceTraversalService(filesystemPort);
        com.contextengine.application.scanner.LanguageDetector languageDetector =
            new com.contextengine.application.scanner.LanguageDetector();
        com.contextengine.application.scanner.FileFilter fileFilter = new com.contextengine.application.scanner.FileFilter();
        com.contextengine.application.scanner.FileDiscoveryService fileDiscoveryService =
            new com.contextengine.application.scanner.FileDiscoveryService(traversalService, fileFilter, languageDetector);
        com.contextengine.application.scanner.WorkspaceScanner workspaceScanner =
            new com.contextengine.application.scanner.WorkspaceScanner(fileDiscoveryService);
        com.contextengine.domain.event.DomainEventPublisher eventPublisher = event -> { /* no-op */ };

        com.contextengine.infrastructure.parser.LanguageParserFactory parserFactory =
            new com.contextengine.infrastructure.parser.LanguageParserFactory();
        com.contextengine.application.scanner.ParserRegistry registry =
            new com.contextengine.application.scanner.ParserRegistry(parserFactory);
        com.contextengine.application.scanner.ParserCoordinator parserCoordinator =
            new com.contextengine.application.scanner.ParserCoordinator(registry);
        com.contextengine.application.scanner.SymbolExtractor symbolExtractor =
            new com.contextengine.application.scanner.SymbolExtractor();

        com.contextengine.application.scanner.incremental.ChangeDetector changeDetector =
            new com.contextengine.application.scanner.incremental.ChangeDetector();
        com.contextengine.application.scanner.dependency.ManifestParser manifestParser =
            new com.contextengine.application.scanner.dependency.ManifestParser();
        com.contextengine.application.scanner.dependency.DependencyScanner dependencyScanner =
            new com.contextengine.application.scanner.dependency.DependencyScanner(filesystemPort, manifestParser);
        com.contextengine.application.scanner.validation.ScannerValidator scannerValidator =
            new com.contextengine.application.scanner.validation.ScannerValidator();

        com.contextengine.application.scanner.ScannerEngine scannerEngine =
            new com.contextengine.application.scanner.ScannerEngine(
                workspaceScanner,
                eventPublisher,
                parserCoordinator,
                symbolExtractor,
                filesystemPort,
                changeDetector,
                dependencyScanner,
                scannerValidator,
                new com.contextengine.application.knowledge.engine.KnowledgeEngineImpl()
            );

        ScanProjectUseCase useCase = new ScanProjectUseCase(projectRepository, filesystemPort, gitPort, scannerEngine);
        ScanProjectCommand cmd = new ScanProjectCommand(project.id(), false, false);

        ApplicationResult<Boolean> result = useCase.execute(cmd);
        if (result.isFailure()) {
            result.error().get().printStackTrace();
        }
        assertThat(result.isSuccess()).isTrue();

        Project updatedProject = projectRepository.findById(project.id()).get();
        assertThat(updatedProject.workspace().activeBranch()).isEqualTo("main");
        assertThat(updatedProject.workspace().activeCommitHash()).isEqualTo("f00b4rhash");
        assertThat(updatedProject.workspace().trackedPaths()).contains(new Path("src/App.java"));
    }

    @Test
    void testCreateFeatureUseCase() {
        Project project = registrationService.registerProject(new Path(System.getProperty("user.dir")), "Test Proj", List.of());
        projectRepository.save(project);

        CreateFeatureUseCase useCase = new CreateFeatureUseCase(projectRepository);
        CreateFeatureCommand cmd = new CreateFeatureCommand(project.id(), "Milestone 1", Priority.HIGH, "details");

        ApplicationResult<FeatureDto> result = useCase.execute(cmd);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().get().title()).isEqualTo("Milestone 1");
        assertThat(result.value().get().priority()).isEqualTo("HIGH");
    }

    @Test
    void testCreateTaskUseCase() {
        Project project = registrationService.registerProject(new Path(System.getProperty("user.dir")), "Test Proj", List.of());
        projectRepository.save(project);

        CreateTaskUseCase useCase = new CreateTaskUseCase(projectRepository);
        CreateTaskCommand cmd = new CreateTaskCommand(project.id(), null, "Do work", Priority.LOW);

        ApplicationResult<TaskDto> result = useCase.execute(cmd);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().get().description()).isEqualTo("Do work");
        assertThat(result.value().get().priority()).isEqualTo("LOW");
    }

    @Test
    void testCreateDecisionUseCase() {
        Project project = registrationService.registerProject(new Path(System.getProperty("user.dir")), "Test Proj", List.of());
        projectRepository.save(project);

        CreateDecisionUseCase useCase = new CreateDecisionUseCase(projectRepository);
        CreateDecisionCommand cmd = new CreateDecisionCommand(project.id(), "ADR-001", new Path("adr.md"), "rationale", "consequences");

        ApplicationResult<DecisionDto> result = useCase.execute(cmd);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().get().title()).isEqualTo("ADR-001");
        assertThat(result.value().get().markdownPath()).isEqualTo("adr.md");
    }

    @Test
    void testGetLatestSnapshotUseCase() {
        ProjectId projId = ProjectId.generate();
        Context context = new Context(projId, new TokenBudget(2000));
        ContextSnapshot snapshot = new ContextSnapshot(
            SnapshotId.generate(),
            projId,
            new Version(1),
            Timestamp.now(),
            new ContextSummary(1, 10, List.of("FILE:1")),
            List.of()
        );
        context.addSnapshot(snapshot);
        contextRepository.save(context);

        GetLatestSnapshotUseCase useCase = new GetLatestSnapshotUseCase(contextRepository);
        GetLatestSnapshotQuery query = new GetLatestSnapshotQuery(projId);

        ApplicationResult<ContextSnapshotDto> result = useCase.execute(query);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().get().tokensUsed()).isEqualTo(10);
    }

    @Test
    void testGenerateContextUseCase() {
        ProjectId projId = ProjectId.generate();
        GenerateContextUseCase useCase = new GenerateContextUseCase(
            contextRepository,
            retrievalEngine,
            rankingEngine,
            contextBuilder,
            tokenBudgetManager,
            contextValidator,
            knowledgeOptimizer,
            eventPublisher
        );
        GenerateContextCommand cmd = new GenerateContextCommand(
            projId,
            new SearchQuery("App", false, new Metadata(Map.of()), 10),
            NodeId.generate(),
            new TokenBudget(2000),
            FormatEnum.MARKDOWN
        );

        ApplicationResult<ContextSnapshotDto> result = useCase.execute(cmd);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().get().tokensUsed()).isEqualTo(100);
    }

    @Test
    void testProjectApplicationServiceRegisterProjectSuccess() {
        RegisterProjectCommand cmd = new RegisterProjectCommand(new Path(System.getProperty("user.dir")), "Test Proj", List.of());
        ApplicationResult<ProjectDto> result = projectApplicationService.registerProject(cmd);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().get().title()).isEqualTo("Test Proj");
    }

    @Test
    void testProjectApplicationServiceRegisterProjectValidationFailure() {
        ApplicationResult<ProjectDto> result = projectApplicationService.registerProject(null);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().get().getMessage()).contains("Validation failed");
    }

    @Test
    void testContextApplicationServiceGenerateContextSuccess() {
        ProjectId projId = ProjectId.generate();
        GenerateContextCommand cmd = new GenerateContextCommand(
            projId,
            new SearchQuery("App", false, new Metadata(Map.of()), 10),
            NodeId.generate(),
            new TokenBudget(2000),
            FormatEnum.MARKDOWN
        );

        ApplicationResult<ContextSnapshotDto> result = contextApplicationService.generateContext(cmd);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().get().tokensUsed()).isEqualTo(100);
    }

    @Test
    void testContextApplicationServiceGenerateContextValidationFailure() {
        ApplicationResult<ContextSnapshotDto> result = contextApplicationService.generateContext(null);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().get().getMessage()).contains("Validation failed");
    }
}
