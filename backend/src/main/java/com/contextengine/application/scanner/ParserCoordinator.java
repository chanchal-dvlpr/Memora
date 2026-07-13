package com.contextengine.application.scanner;

import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.valueobject.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Coordinates selecting the correct parser engine, executing the parser, and managing parse failures safely.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * Dependencies: ParserRegistryBroker, Result, KnowledgeNode
 * </p>
 */
public class ParserCoordinator {

    private final ParserRegistryBroker parserRegistryBroker;

    /**
     * Constructs a ParserCoordinator.
     *
     * @param parserRegistryBroker the parser registry broker
     */
    public ParserCoordinator(ParserRegistryBroker parserRegistryBroker) {
        this.parserRegistryBroker = Objects.requireNonNull(parserRegistryBroker, "ParserRegistryBroker must not be null");
    }

    /**
     * Resolves the appropriate parser for the candidate and parses its content.
     * Handles syntax errors or parsing exceptions safely using non-blocking error recovery policies.
     *
     * @param candidate the scan candidate to parse
     * @param fileContent the raw text content of the file
     * @return collection of parsed symbol nodes, or an empty collection in case of failure
     */
    public Collection<KnowledgeNode> parse(ScanCandidate candidate, String fileContent) {
        Objects.requireNonNull(candidate, "ScanCandidate must not be null");
        Objects.requireNonNull(fileContent, "FileContent must not be null");

        // Extract extension from relative path
        String relativePath = candidate.relativePath();
        int lastDot = relativePath.lastIndexOf('.');
        if (lastDot == -1) {
            return Collections.emptyList();
        }
        String ext = relativePath.substring(lastDot + 1);

        Result<ParserConfiguration, UnsupportedExtensionException> resolveResult = 
            parserRegistryBroker.resolveParserForFile(ext);

        if (resolveResult.isFailure()) {
            // Safe fallback: unsupported extension behaves like plain text (quarantine or skip)
            return Collections.emptyList();
        }

        ParserConfiguration config = resolveResult.value().get();
        try {
            return config.parserInstance().parse(new Path(candidate.relativePath()), fileContent);
        } catch (Exception e) {
            // Error Recovery Policy: Non-blocking fallback to stale / empty recovery
            // System logs the quarantine state of the failed parse job
            System.err.println("[PARSER-COORDINATOR] Safe quarantine triggered for " + candidate.relativePath() + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
