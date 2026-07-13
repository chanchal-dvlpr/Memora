package com.contextengine.application.scanner;

import com.contextengine.application.port.FilesystemPort;
import com.contextengine.application.scanner.dependency.DependencyScanner;
import com.contextengine.application.scanner.dependency.ManifestParser;
import com.contextengine.application.scanner.dependency.ProjectDependency;
import com.contextengine.application.scanner.incremental.ChangeDetector;
import com.contextengine.application.scanner.incremental.FileFingerprint;
import com.contextengine.application.scanner.incremental.ScanDelta;
import com.contextengine.application.scanner.validation.ScannerValidator;
import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.entity.Project;
import com.contextengine.domain.event.DomainEvent;
import com.contextengine.domain.event.DomainEventPublisher;
import com.contextengine.domain.event.ProjectScanned;
import com.contextengine.domain.event.ScanCompleted;
import com.contextengine.domain.event.ScanStarted;
import com.contextengine.domain.valueobject.Metadata;
import com.contextengine.domain.valueobject.NodeId;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.infrastructure.parser.LanguageParserFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectScannerIntegrityTest {

    private FilesystemPort filesystemPort;
    private Project project;
    private DomainEventPublisher eventPublisher;
    private List<DomainEvent> publishedEvents;
    private LanguageDetector languageDetector;

    @BeforeEach
    void setUp() {
        publishedEvents = new ArrayList<>();
        eventPublisher = publishedEvents::add;
        languageDetector = new LanguageDetector();

        project = new Project(
            ProjectId.generate(),
            new Path(System.getProperty("user.dir")),
            "Scanner Integrity Project"
        );

        filesystemPort = new FilesystemPort() {
            @Override
            public boolean exists(Path path) {
                return true;
            }

            @Override
            public boolean isDirectory(Path path) {
                return false;
            }

            @Override
            public boolean hasReadWritePermissions(Path path) {
                return true;
            }

            @Override
            public List<Path> listFiles(Path root, List<String> exclusions) {
                return List.of(
                    new Path("src/main/java/Main.java"),
                    new Path("node_modules/express/index.js"),
                    new Path("target/classes/Main.class"),
                    new Path("docs/specs.pdf"),
                    new Path("src/main/resources/config.properties")
                );
            }

            @Override
            public String readFile(Path filePath) {
                if (filePath.value().endsWith("Main.java")) {
                    return "public class Main {\n  public void execute() {}\n}";
                }
                if (filePath.value().endsWith("pom.xml")) {
                    return "<dependency>\n  <groupId>org.springframework</groupId>\n  <artifactId>spring-context</artifactId>\n  <version>6.1.0</version>\n</dependency>";
                }
                if (filePath.value().endsWith("package.json")) {
                    return "{\n  \"dependencies\": {\n    \"express\": \"^4.18.2\"\n  }\n}";
                }
                if (filePath.value().endsWith("requirements.txt")) {
                    return "requests==2.28.1\nflask";
                }
                return "";
            }
        };
    }

    @Test
    void testIgnoreRuleProcessorDefaultsAndProjectExclusions() {
        IgnoreRuleProcessor processor = new IgnoreRuleProcessor(project, List.of("**/docs/**", "docs/**"));

        // Default ignored directories check
        assertThat(processor.shouldIgnore("node_modules/express/index.js", false)).isTrue();
        assertThat(processor.shouldIgnore("target/classes/Main.class", false)).isTrue();

        // Custom project-level exclusion check
        assertThat(processor.shouldIgnore("docs/specs.pdf", false)).isTrue();

        // Valid source files check
        assertThat(processor.shouldIgnore("src/main/java/Main.java", false)).isFalse();
    }

    @Test
    void testFileFilterSizeAndBinaryExclusions() {
        FileFilter filter = new FileFilter();

        ScanCandidate validCandidate = new ScanCandidate(
            "src/main/java/Main.java",
            "/absolute/src/main/java/Main.java",
            1024,
            Instant.now(),
            "FILE",
            SupportedLanguage.JAVA
        );

        ScanCandidate tooLarge = new ScanCandidate(
            "src/large.log",
            "/absolute/src/large.log",
            ScannerConstants.MAX_FILE_SIZE_BYTES + 100,
            Instant.now(),
            "FILE",
            SupportedLanguage.UNSUPPORTED
        );

        ScanCandidate binaryFile = new ScanCandidate(
            "images/logo.png",
            "/absolute/images/logo.png",
            5000,
            Instant.now(),
            "FILE",
            SupportedLanguage.UNSUPPORTED
        );

        ScanCandidate dirCandidate = new ScanCandidate(
            "src/main/java",
            "/absolute/src/main/java",
            0,
            Instant.now(),
            "DIR",
            SupportedLanguage.UNSUPPORTED
        );

        assertThat(filter.accept(validCandidate)).isTrue();
        assertThat(filter.accept(tooLarge)).isFalse();
        assertThat(filter.accept(binaryFile)).isFalse();
        assertThat(filter.accept(dirCandidate)).isFalse();
    }

    @Test
    void testLanguageDetectorMapping() {
        assertThat(languageDetector.detect("Main.java")).isEqualTo(SupportedLanguage.JAVA);
        assertThat(languageDetector.detect("app.py")).isEqualTo(SupportedLanguage.PYTHON);
        assertThat(languageDetector.detect("index.ts")).isEqualTo(SupportedLanguage.TYPESCRIPT);
        assertThat(languageDetector.detect("index.js")).isEqualTo(SupportedLanguage.JAVASCRIPT);
        assertThat(languageDetector.detect("library.cpp")).isEqualTo(SupportedLanguage.CPP);
        assertThat(languageDetector.detect("main.go")).isEqualTo(SupportedLanguage.GO);
        assertThat(languageDetector.detect("README.md")).isEqualTo(SupportedLanguage.UNSUPPORTED);

        // Custom registration
        languageDetector.register("rs", SupportedLanguage.UNSUPPORTED);
        assertThat(languageDetector.detect("lib.rs")).isEqualTo(SupportedLanguage.UNSUPPORTED);
    }

    @Test
    void testParserRegistryResolutionAndConflict() {
        LanguageParserFactory parserFactory = new LanguageParserFactory();
        ParserRegistry registry = new ParserRegistry(parserFactory);

        // Resolve default fallback
        Result<ParserConfiguration, UnsupportedExtensionException> res1 = registry.resolveParserForFile("java");
        assertThat(res1.isSuccess()).isTrue();
        assertThat(res1.value().get().parserName()).isEqualTo("TSParserBridge");

        // Conflict check
        Result<Void, RegistrationConflictException> registerResult = registry.registerParserEngine(
            "CustomJavaParser",
            List.of("java"),
            new ParserCapabilityMatrix(Map.of("ast", true))
        );
        assertThat(registerResult.isFailure()).isTrue();
        assertThat(registerResult.error().get().getMessage()).contains("Conflict detected");
    }

    @Test
    void testParserCoordinatorQuarantine() {
        ParserRegistryBroker broker = new ParserRegistryBroker() {
            @Override
            public Result<ParserConfiguration, UnsupportedExtensionException> resolveParserForFile(String fileExtension) {
                return Result.success(new ParserConfiguration(
                    "ErrorParser",
                    List.of(fileExtension),
                    new ParserCapabilityMatrix(Map.of()),
                    (filePath, fileContent) -> {
                        throw new RuntimeException("Parser crash test");
                    }
                ));
            }

            @Override
            public Result<Void, RegistrationConflictException> registerParserEngine(
                String parserName, List<String> targetExtensions, ParserCapabilityMatrix capabilities
            ) {
                return Result.success(null);
            }
        };

        ParserCoordinator coordinator = new ParserCoordinator(broker);
        ScanCandidate candidate = new ScanCandidate(
            "Crash.java",
            "/absolute/Crash.java",
            100,
            Instant.now(),
            "FILE",
            SupportedLanguage.JAVA
        );

        // Verify exception is handled safely and returns empty collection
        Collection<KnowledgeNode> nodes = coordinator.parse(candidate, "class Crash {}");
        assertThat(nodes).isEmpty();
    }

    @Test
    void testSymbolExtractionNormalization() {
        SymbolExtractor extractor = new SymbolExtractor();
        KnowledgeNode classNode = new KnowledgeNode(
            NodeId.generate(),
            "CODE_SYMBOL",
            new Metadata(Map.of(
                "name", "JSONRPCTransport",
                "kind", "CLASS",
                "path", "src/Transport.java",
                "line", "12",
                "end_line", "45",
                "modifiers", "PUBLIC"
            ))
        );

        Collection<SourceSymbol> symbols = extractor.extract(List.of(classNode));
        assertThat(symbols).hasSize(1);
        SourceSymbol symbol = symbols.iterator().next();

        assertThat(symbol.name()).isEqualTo("JSONRPCTransport");
        assertThat(symbol.kind()).isEqualTo("CLASS");
        assertThat(symbol.filePath()).isEqualTo("src/Transport.java");
        assertThat(symbol.startLine()).isEqualTo(12);
        assertThat(symbol.endLine()).isEqualTo(45);
        assertThat(symbol.metadata().get("modifiers")).isEqualTo("PUBLIC");
    }

    @Test
    void testManifestParserPomPackageRequirements() {
        ManifestParser parser = new ManifestParser();

        Collection<ProjectDependency> pomDeps = parser.parse(
            "pom.xml",
            "<dependency>\n  <groupId>org.springframework</groupId>\n  <artifactId>spring-context</artifactId>\n  <version>6.1.0</version>\n  <scope>test</scope>\n</dependency>"
        );
        assertThat(pomDeps).hasSize(1);
        ProjectDependency pomDep = pomDeps.iterator().next();
        assertThat(pomDep.name()).isEqualTo("org.springframework:spring-context");
        assertThat(pomDep.version()).isEqualTo("6.1.0");
        assertThat(pomDep.type()).isEqualTo("MAVEN");
        assertThat(pomDep.scope()).isEqualTo("TEST");

        Collection<ProjectDependency> npmDeps = parser.parse(
            "package.json",
            "{\n  \"dependencies\": {\n    \"express\": \"^4.18.2\"\n  },\n  \"devDependencies\": {\n    \"typescript\": \"~5.0.4\"\n  }\n}"
        );
        assertThat(npmDeps).hasSize(2);
        assertThat(npmDeps).extracting(ProjectDependency::name).containsExactlyInAnyOrder("express", "typescript");
        assertThat(npmDeps).filteredOn(d -> d.name().equals("express"))
            .extracting(ProjectDependency::version, ProjectDependency::scope)
            .containsExactly(org.assertj.core.groups.Tuple.tuple("4.18.2", "PROD"));

        Collection<ProjectDependency> pipDeps = parser.parse(
            "requirements.txt",
            "requests==2.28.1\nflask"
        );
        assertThat(pipDeps).hasSize(2);
        assertThat(pipDeps).extracting(ProjectDependency::name, ProjectDependency::version)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple("requests", "2.28.1"),
                org.assertj.core.groups.Tuple.tuple("flask", "LATEST")
            );
    }

    @Test
    void testChangeDetectorAddedModifiedDeleted() {
        ChangeDetector detector = new ChangeDetector();
        String projectId = "test-proj";

        ScanCandidate c1 = new ScanCandidate("file1.java", "/abs/file1.java", 100, Instant.ofEpochMilli(1000), "FILE", SupportedLanguage.JAVA);
        ScanCandidate c2 = new ScanCandidate("file2.java", "/abs/file2.java", 200, Instant.ofEpochMilli(2000), "FILE", SupportedLanguage.JAVA);

        // Baseline scan: cache is empty
        ScanDelta d1 = detector.detect(projectId, List.of(c1, c2));
        assertThat(d1.added()).containsExactlyInAnyOrder(c1, c2);
        assertThat(d1.modified()).isEmpty();
        assertThat(d1.deleted()).isEmpty();

        // Update cache
        detector.update(projectId, List.of(c1, c2));

        // Subrun: c2 modified, c1 deleted, c3 added
        ScanCandidate c2Mod = new ScanCandidate("file2.java", "/abs/file2.java", 250, Instant.ofEpochMilli(2050), "FILE", SupportedLanguage.JAVA);
        ScanCandidate c3 = new ScanCandidate("file3.java", "/abs/file3.java", 300, Instant.ofEpochMilli(3000), "FILE", SupportedLanguage.JAVA);

        ScanDelta d2 = detector.detect(projectId, List.of(c2Mod, c3));
        assertThat(d2.added()).containsExactly(c3);
        assertThat(d2.modified()).containsExactly(c2Mod);
        assertThat(d2.deleted()).containsExactly("file1.java");
    }

    @Test
    void testScannerValidatorRules() {
        ScannerValidator validator = new ScannerValidator();

        // 1. Project state consistency (archived should fail)
        Project archivedProj = new Project(
            ProjectId.generate(),
            new Path(System.getProperty("user.dir")),
            "Archived Project"
        );
        archivedProj.archive();
        assertThatThrownBy(() -> validator.validateProjectState(archivedProj))
            .isInstanceOf(ScannerException.class)
            .hasMessageContaining("Cannot scan archived project");

        // 2. Confinement boundary check
        validator.validateConfinement("/root/boundary", "/root/boundary/src/Main.java");
        assertThatThrownBy(() -> validator.validateConfinement("/root/boundary", "/other/boundary/src/Main.java"))
            .isInstanceOf(ScannerException.class)
            .hasMessageContaining("path resolves outside the confinement boundary");
    }

    @Test
    void testWorkspaceScannerWorkflowAndMetrics() {
        WorkspaceTraversalService traversalService = new WorkspaceTraversalService(filesystemPort);
        FileFilter filter = new FileFilter();
        FileDiscoveryService discoveryService = new FileDiscoveryService(traversalService, filter, languageDetector);
        WorkspaceScanner scanner = new WorkspaceScanner(discoveryService);

        ScannerContext context = new ScannerContext(
            new ScanSession(project.id().value(), "FULL"),
            project
        );

        Collection<ScanCandidate> result = scanner.scan(context);

        // Verify scannable source files are discovered and binary/ignored ones are filtered
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ScanCandidate::relativePath)
            .containsExactlyInAnyOrder("src/main/java/Main.java", "src/main/resources/config.properties");

        // Verify language mapping correctly attached to candidate
        assertThat(result).filteredOn(c -> c.relativePath().endsWith(".java"))
            .extracting(ScanCandidate::language)
            .containsExactly(SupportedLanguage.JAVA);

        // Verify session metrics populated correctly
        ScanSession session = context.getSession();
        assertThat(session.getFileCount()).isEqualTo(2);
        assertThat(session.getDirectoryCount()).isEqualTo(1); // incremented root dir count
    }

    @Test
    void testScannerEngineIntegrationAndLifecycle() {
        // Setup filesystem mocks with a manifest file included
        FilesystemPort customFs = new FilesystemPort() {
            @Override
            public boolean exists(Path path) {
                return true;
            }

            @Override
            public boolean isDirectory(Path path) {
                return false;
            }

            @Override
            public boolean hasReadWritePermissions(Path path) {
                return true;
            }

            @Override
            public List<Path> listFiles(Path root, List<String> exclusions) {
                return List.of(
                    new Path("src/main/java/Main.java"),
                    new Path("pom.xml")
                );
            }

            @Override
            public String readFile(Path filePath) {
                if (filePath.value().endsWith("Main.java")) {
                    return "public class Main {\n  public void execute() {}\n}";
                }
                if (filePath.value().endsWith("pom.xml")) {
                    return "<dependency>\n  <groupId>org.springframework</groupId>\n  <artifactId>spring-context</artifactId>\n  <version>6.1.0</version>\n</dependency>";
                }
                return "";
            }
        };

        WorkspaceTraversalService traversalService = new WorkspaceTraversalService(customFs);
        FileFilter filter = new FileFilter();
        FileDiscoveryService discoveryService = new FileDiscoveryService(traversalService, filter, languageDetector);
        WorkspaceScanner scanner = new WorkspaceScanner(discoveryService);

        LanguageParserFactory parserFactory = new LanguageParserFactory();
        ParserRegistry registry = new ParserRegistry(parserFactory);
        ParserCoordinator coordinator = new ParserCoordinator(registry);
        SymbolExtractor extractor = new SymbolExtractor();
        ChangeDetector changeDetector = new ChangeDetector();
        ManifestParser manifestParser = new ManifestParser();
        DependencyScanner dependencyScanner = new DependencyScanner(customFs, manifestParser);
        ScannerValidator scannerValidator = new ScannerValidator();

        ScannerEngine engine = new ScannerEngine(
            scanner,
            eventPublisher,
            coordinator,
            extractor,
            customFs,
            changeDetector,
            dependencyScanner,
            scannerValidator
        );

        ScanSession session = engine.scan(project, "FULL");

        assertThat(session.getState()).isEqualTo(ScanSession.State.COMPLETED);
        
        // Verify lifecycle events (ScanStarted & ScanCompleted) published
        assertThat(publishedEvents).extracting(Object::getClass)
            .contains(ScanStarted.class, ScanCompleted.class);

        ScanCompleted scanCompletedEvent = (ScanCompleted) publishedEvents.stream()
            .filter(e -> e instanceof ScanCompleted)
            .findFirst()
            .orElseThrow();
        assertThat(scanCompletedEvent.projectId()).isEqualTo(project.id());
        assertThat(scanCompletedEvent.scanMode()).isEqualTo("FULL");
        assertThat(scanCompletedEvent.filesScannedCount()).isEqualTo(2);
    }
}
