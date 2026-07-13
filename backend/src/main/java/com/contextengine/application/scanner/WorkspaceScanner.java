package com.contextengine.application.scanner;

import java.util.Collection;
import java.util.Objects;

/**
 * High-level coordinator coordinating directory crawls and file discovery filtering.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * </p>
 */
public class WorkspaceScanner {

    private final FileDiscoveryService fileDiscoveryService;

    /**
     * Constructs a WorkspaceScanner.
     *
     * @param fileDiscoveryService file discovery service
     */
    public WorkspaceScanner(FileDiscoveryService fileDiscoveryService) {
        this.fileDiscoveryService = Objects.requireNonNull(fileDiscoveryService, "FileDiscoveryService must not be null");
    }

    /**
     * Performs a complete scan discovery of the workspace under the scanner context.
     *
     * @param context active scanner context
     * @return collection of discovered scan candidates
     */
    public Collection<ScanCandidate> scan(ScannerContext context) {
        Objects.requireNonNull(context, "ScannerContext must not be null");

        Collection<ScanCandidate> candidates = fileDiscoveryService.discover(context);

        // Update session metrics
        ScanSession session = context.getSession();
        for (ScanCandidate candidate : candidates) {
            session.incrementFiles();
            session.addBytes(candidate.size());
        }

        return candidates;
    }
}
