package com.contextengine.application.scanner;

import com.contextengine.application.port.FilesystemPort;
import com.contextengine.application.scanner.dependency.DependencyScanner;
import com.contextengine.application.scanner.dependency.ProjectDependency;
import com.contextengine.application.scanner.incremental.ChangeDetector;
import com.contextengine.application.scanner.incremental.ScanDelta;
import com.contextengine.application.scanner.validation.ScannerValidator;
import com.contextengine.domain.entity.Project;
import com.contextengine.domain.event.DomainEventPublisher;
import com.contextengine.domain.event.ProjectScanned;
import com.contextengine.domain.event.ScanCompleted;
import com.contextengine.domain.event.ScanStarted;
import com.contextengine.domain.valueobject.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Entry point orchestration engine that manages scan sessions, transitions session states,
 * and coordinates workspace traversal, incremental filtering, validation, and event publishing.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * Dependencies: WorkspaceScanner, DomainEventPublisher, ParserCoordinator, SymbolExtractor,
 * FilesystemPort, ChangeDetector, DependencyScanner, ScannerValidator
 * </p>
 */
public class ScannerEngine {

    private final WorkspaceScanner workspaceScanner;
    private final DomainEventPublisher eventPublisher;
    private final ParserCoordinator parserCoordinator;
    private final SymbolExtractor symbolExtractor;
    private final FilesystemPort filesystemPort;
    private final ChangeDetector changeDetector;
    private final DependencyScanner dependencyScanner;
    private final ScannerValidator scannerValidator;
    private final com.contextengine.application.knowledge.engine.KnowledgeEngine knowledgeEngine;

    /**
     * Constructs a ScannerEngine.
     *
     * @param workspaceScanner workspace scanner coordinator
     * @param eventPublisher domain event publisher
     * @param parserCoordinator parser coordinator service
     * @param symbolExtractor symbol extractor service
     * @param filesystemPort physical file system port
     * @param changeDetector incremental change detector
     * @param dependencyScanner dependency analysis scanner
     * @param scannerValidator security and state validator
     * @param knowledgeEngine knowledge engine coordinator foundation
     */
    public ScannerEngine(
        WorkspaceScanner workspaceScanner,
        DomainEventPublisher eventPublisher,
        ParserCoordinator parserCoordinator,
        SymbolExtractor symbolExtractor,
        FilesystemPort filesystemPort,
        ChangeDetector changeDetector,
        DependencyScanner dependencyScanner,
        ScannerValidator scannerValidator,
        com.contextengine.application.knowledge.engine.KnowledgeEngine knowledgeEngine
    ) {
        this.workspaceScanner = Objects.requireNonNull(workspaceScanner, "WorkspaceScanner must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "DomainEventPublisher must not be null");
        this.parserCoordinator = Objects.requireNonNull(parserCoordinator, "ParserCoordinator must not be null");
        this.symbolExtractor = Objects.requireNonNull(symbolExtractor, "SymbolExtractor must not be null");
        this.filesystemPort = Objects.requireNonNull(filesystemPort, "FilesystemPort must not be null");
        this.changeDetector = Objects.requireNonNull(changeDetector, "ChangeDetector must not be null");
        this.dependencyScanner = Objects.requireNonNull(dependencyScanner, "DependencyScanner must not be null");
        this.scannerValidator = Objects.requireNonNull(scannerValidator, "ScannerValidator must not be null");
        this.knowledgeEngine = Objects.requireNonNull(knowledgeEngine, "KnowledgeEngine must not be null");
    }

    /**
     * Executes a scanning operation for the target project.
     *
     * @param project target project aggregate
     * @param scanMode scan modality (FULL or INCREMENTAL)
     * @return the scan session tracking metrics
     */
    public ScanSession scan(Project project, String scanMode) {
        Objects.requireNonNull(project, "Project must not be null");
        Objects.requireNonNull(scanMode, "ScanMode must not be null");

        // 1. Enforce state consistency (VR-001)
        scannerValidator.validateProjectState(project);

        ScanSession session = new ScanSession(project.id().value(), scanMode);
        session.transitionTo(ScanSession.State.QUEUED);

        ScannerContext context = new ScannerContext(session, project);
        session.transitionTo(ScanSession.State.SCANNING);

        // Publish ScanStarted event
        eventPublisher.publish(new ScanStarted(project.id(), scanMode, Instant.now()));

        try {
            // 2. Discover workspace candidates
            Collection<ScanCandidate> candidates = workspaceScanner.scan(context);

            // Detect monorepo configurations
            com.contextengine.application.scanner.workspace.MonorepoDetector monorepoDetector = 
                new com.contextengine.application.scanner.workspace.MonorepoDetector();
            com.contextengine.application.scanner.workspace.MonorepoDescriptor monorepoDescriptor = 
                monorepoDetector.detect(project.rootDirectory().value(), candidates, filesystemPort);

            // Integrate with session statistics
            for (int i = 0; i < monorepoDescriptor.detectedModules().size(); i++) {
                session.incrementDirectories();
            }

            System.out.println("[SCANNER-ENGINE] Detected monorepo build type: " + monorepoDescriptor.workspaceType() 
                + " with " + monorepoDescriptor.detectedModules().size() + " modules");

            // 3. Enforce confinement boundaries (SEC-005) & Symlink Loop checks (SEC-004)
            String rootPath = project.rootDirectory().value();
            for (ScanCandidate candidate : candidates) {
                scannerValidator.validateConfinement(rootPath, candidate.absolutePath());
                scannerValidator.validateSymlink(candidate.absolutePath());
            }

            // 4. Calculate incremental scan delta
            ScanDelta delta = changeDetector.detect(project.id().value().toString(), candidates);
            Collection<ScanCandidate> filesToProcess;

            com.contextengine.domain.entity.Workspace workspace = project.workspace();

            if (scanMode.equalsIgnoreCase("FULL")) {
                // Clear all previous tracked paths in workspace
                if (workspace != null) {
                    List<Path> copy = new ArrayList<>(workspace.trackedPaths());
                    for (Path p : copy) {
                        workspace.untrackPath(p);
                    }
                }
                changeDetector.clear(project.id().value().toString());
                filesToProcess = candidates;
            } else {
                // Incremental Scan: Process only added & modified files
                filesToProcess = new ArrayList<>();
                filesToProcess.addAll(delta.added());
                filesToProcess.addAll(delta.modified());

                // Untrack deleted files from the workspace
                if (workspace != null) {
                    for (String deletedPath : delta.deleted()) {
                        workspace.untrackPath(new Path(deletedPath));
                    }
                }
            }

            int totalSymbolsCount = 0;
            List<SourceSymbol> allSymbols = new ArrayList<>();
            if (workspace != null) {
                for (ScanCandidate candidate : filesToProcess) {
                    workspace.trackPath(new Path(candidate.relativePath()));

                    // Read content for parsing
                    String fileContent = "";
                    try {
                        fileContent = filesystemPort.readFile(new Path(candidate.absolutePath()));
                    } catch (Exception e) {
                        System.err.println("[SCANNER-ENGINE] Failed to read file content for " + candidate.relativePath() + ": " + e.getMessage());
                    }

                    // Parse the candidate code structure
                    Collection<com.contextengine.domain.entity.KnowledgeNode> nodes = 
                        parserCoordinator.parse(candidate, fileContent);

                    // Extract structural symbols
                    Collection<SourceSymbol> symbols = symbolExtractor.extract(nodes);
                    allSymbols.addAll(symbols);
                    totalSymbolsCount += symbols.size();
                }
            }

            // 5. Run Dependency Analysis
            Collection<ProjectDependency> dependencies = dependencyScanner.scan(candidates);
            System.out.println("[SCANNER-ENGINE] Extracted project dependencies: " + dependencies.size());

            // 6. Update incremental change fingerprints cache
            changeDetector.update(project.id().value().toString(), candidates);

            // 7. Run Structural Hashing
            com.contextengine.application.scanner.hashing.StructuralHasher structuralHasher = 
                new com.contextengine.application.scanner.hashing.StructuralHasher(filesystemPort);
            com.contextengine.application.scanner.hashing.StructuralHashResult hashResult = 
                structuralHasher.calculate(project.rootDirectory().value(), candidates);
            session.setCompositeStructuralHash(hashResult.workspaceHash());

            System.out.println("[SCANNER-ENGINE] Generated composite structural hash: " + hashResult.workspaceHash());

            session.transitionTo(ScanSession.State.COMPLETED);

            // 8. Invoke Knowledge Engine Foundation (Phase 10.1)
            java.util.Map<String, Object> scannerStats = new java.util.HashMap<>();
            scannerStats.put("filesCount", session.getFileCount());
            scannerStats.put("symbolsCount", (long) totalSymbolsCount);
            scannerStats.put("directoriesCount", session.getDirectoryCount());

            // Collect delta information
            Collection<String> addedPaths = new ArrayList<>();
            Collection<String> modifiedPaths = new ArrayList<>();
            Collection<String> deletedPaths = new ArrayList<>();
            if (delta != null) {
                for (ScanCandidate c : delta.added()) {
                    addedPaths.add(c.relativePath());
                }
                for (ScanCandidate c : delta.modified()) {
                    modifiedPaths.add(c.relativePath());
                }
                deletedPaths.addAll(delta.deleted());
            }

            com.contextengine.application.knowledge.engine.KnowledgeEngineContext engineContext = 
                new com.contextengine.application.knowledge.engine.KnowledgeEngineContext(
                    project.id().value().toString(),
                    workspace != null ? workspace.id().value().toString() : "default-workspace",
                    session.getScanId().toString(),
                    hashResult.workspaceHash(),
                    session.getStartTime(),
                    scannerStats,
                    new com.contextengine.application.knowledge.engine.KnowledgeEngineConfiguration(),
                    candidates,
                    allSymbols,
                    dependencies,
                    addedPaths,
                    modifiedPaths,
                    deletedPaths,
                    "INCREMENTAL".equalsIgnoreCase(scanMode)
                );
            
            com.contextengine.application.knowledge.engine.KnowledgeEngineResult engineResult = 
                knowledgeEngine.process(engineContext);
            
            System.out.println("[SCANNER-ENGINE] Knowledge Engine invocation status: " + engineResult.processingStatus());

            // Activate project upon initial scan completion
            project.activate();

            // Publish completed events
            ProjectScanned scannedEvent = new ProjectScanned(
                project.id(),
                (int) session.getFileCount(),
                totalSymbolsCount,
                Instant.now()
            );
            eventPublisher.publish(scannedEvent);

            ScanCompleted completedEvent = new ScanCompleted(
                project.id(),
                scanMode,
                (int) session.getFileCount(),
                totalSymbolsCount,
                Instant.now()
            );
            eventPublisher.publish(completedEvent);

            return session;
        } catch (Exception e) {
            session.transitionTo(ScanSession.State.FAILED);
            throw new ScannerException("Scanner execution failed for project: " + project.id().value(), e);
        }
    }
}
