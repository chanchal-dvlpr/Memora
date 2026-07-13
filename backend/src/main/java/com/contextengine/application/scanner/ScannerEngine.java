package com.contextengine.application.scanner;

import com.contextengine.application.port.FilesystemPort;
import com.contextengine.domain.entity.Project;
import com.contextengine.domain.event.DomainEventPublisher;
import com.contextengine.domain.event.ProjectScanned;
import com.contextengine.domain.valueobject.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;

/**
 * Entry point orchestration engine that manages scan sessions, transitions session states,
 * and coordinates workspace traversal with event publishing.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * Dependencies: WorkspaceScanner, DomainEventPublisher, ParserCoordinator, SymbolExtractor, FilesystemPort
 * </p>
 */
public class ScannerEngine {

    private final WorkspaceScanner workspaceScanner;
    private final DomainEventPublisher eventPublisher;
    private final ParserCoordinator parserCoordinator;
    private final SymbolExtractor symbolExtractor;
    private final FilesystemPort filesystemPort;

    /**
     * Constructs a ScannerEngine.
     *
     * @param workspaceScanner workspace scanner coordinator
     * @param eventPublisher domain event publisher
     * @param parserCoordinator parser coordinator service
     * @param symbolExtractor symbol extractor service
     * @param filesystemPort physical file system port
     */
    public ScannerEngine(
        WorkspaceScanner workspaceScanner,
        DomainEventPublisher eventPublisher,
        ParserCoordinator parserCoordinator,
        SymbolExtractor symbolExtractor,
        FilesystemPort filesystemPort
    ) {
        this.workspaceScanner = Objects.requireNonNull(workspaceScanner, "WorkspaceScanner must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "DomainEventPublisher must not be null");
        this.parserCoordinator = Objects.requireNonNull(parserCoordinator, "ParserCoordinator must not be null");
        this.symbolExtractor = Objects.requireNonNull(symbolExtractor, "SymbolExtractor must not be null");
        this.filesystemPort = Objects.requireNonNull(filesystemPort, "FilesystemPort must not be null");
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

        ScanSession session = new ScanSession(project.id().value(), scanMode);
        session.transitionTo(ScanSession.State.QUEUED);

        ScannerContext context = new ScannerContext(session, project);
        session.transitionTo(ScanSession.State.SCANNING);

        try {
            Collection<ScanCandidate> candidates = workspaceScanner.scan(context);

            int totalSymbolsCount = 0;
            com.contextengine.domain.entity.Workspace workspace = project.workspace();
            if (workspace != null) {
                for (ScanCandidate candidate : candidates) {
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
                    totalSymbolsCount += symbols.size();
                }
            }

            session.transitionTo(ScanSession.State.COMPLETED);

            // Activate project upon initial scan completion
            project.activate();

            // Publish completed event
            ProjectScanned scannedEvent = new ProjectScanned(
                project.id(),
                (int) session.getFileCount(),
                totalSymbolsCount,
                Instant.now()
            );
            eventPublisher.publish(scannedEvent);

            return session;
        } catch (Exception e) {
            session.transitionTo(ScanSession.State.FAILED);
            throw new ScannerException("Scanner execution failed for project: " + project.id().value(), e);
        }
    }
}
