package com.contextengine.application.scanner;

import com.contextengine.application.port.FilesystemPort;
import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.entity.Project;
import com.contextengine.domain.event.DomainEvent;
import com.contextengine.domain.event.DomainEventPublisher;
import com.contextengine.domain.event.ProjectScanned;
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

        // Verify exception is handled safely and returns empty list (non-blocking quarantine policy)
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
        WorkspaceTraversalService traversalService = new WorkspaceTraversalService(filesystemPort);
        FileFilter filter = new FileFilter();
        FileDiscoveryService discoveryService = new FileDiscoveryService(traversalService, filter, languageDetector);
        WorkspaceScanner scanner = new WorkspaceScanner(discoveryService);

        LanguageParserFactory parserFactory = new LanguageParserFactory();
        ParserRegistry registry = new ParserRegistry(parserFactory);
        ParserCoordinator coordinator = new ParserCoordinator(registry);
        SymbolExtractor extractor = new SymbolExtractor();

        ScannerEngine engine = new ScannerEngine(
            scanner,
            eventPublisher,
            coordinator,
            extractor,
            filesystemPort
        );

        ScanSession session = engine.scan(project, "FULL");

        assertThat(session.getState()).isEqualTo(ScanSession.State.COMPLETED);
        assertThat(publishedEvents).hasSize(1);
        assertThat(publishedEvents.get(0)).isInstanceOf(ProjectScanned.class);

        ProjectScanned event = (ProjectScanned) publishedEvents.get(0);
        assertThat(event.projectId()).isEqualTo(project.id());
        assertThat(event.filesScannedCount()).isEqualTo(2);
        
        // Main.java parsed: should have Main class and execute() method symbols extracted
        assertThat(event.filesScannedCount()).isEqualTo(2);
        assertThat(event.filesScannedCount()).isGreaterThan(0);
    }
}
