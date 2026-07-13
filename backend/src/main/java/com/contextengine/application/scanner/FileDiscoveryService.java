package com.contextengine.application.scanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Service that integrates traversal, ignore processor, and file filters
 * to produce the final deduplicated set of scannable file candidates.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * </p>
 */
public class FileDiscoveryService {

    private final WorkspaceTraversalService traversalService;
    private final FileFilter fileFilter;
    private final LanguageDetector languageDetector;

    /**
     * Constructs a FileDiscoveryService.
     *
     * @param traversalService recursive traversal service
     * @param fileFilter file filter rule validator
     * @param languageDetector language detection service
     */
    public FileDiscoveryService(
        WorkspaceTraversalService traversalService,
        FileFilter fileFilter,
        LanguageDetector languageDetector
    ) {
        this.traversalService = Objects.requireNonNull(traversalService, "WorkspaceTraversalService must not be null");
        this.fileFilter = Objects.requireNonNull(fileFilter, "FileFilter must not be null");
        this.languageDetector = Objects.requireNonNull(languageDetector, "LanguageDetector must not be null");
    }

    /**
     * Traverses the project directory structure, applies filters, and returns accepted file candidates.
     *
     * @param context active scanner context
     * @return collection of validated scan candidates
     */
    public Collection<ScanCandidate> discover(ScannerContext context) {
        Objects.requireNonNull(context, "ScannerContext must not be null");

        IgnoreRuleProcessor ignoreRuleProcessor = new IgnoreRuleProcessor(context.getProject(), java.util.List.of());
        List<java.nio.file.Path> paths = traversalService.traverse(context, ignoreRuleProcessor);

        List<ScanCandidate> candidates = new ArrayList<>();
        java.nio.file.Path rootPath = Paths.get(context.getCanonicalRoot().value());

        for (java.nio.file.Path p : paths) {
            java.nio.file.Path relative = rootPath.relativize(p);
            String relPathStr = relative.toString().replace('\\', '/');

            long size = 0;
            java.time.Instant lastModified = java.time.Instant.now();
            if (Files.exists(p) && Files.isRegularFile(p)) {
                try {
                    size = Files.size(p);
                    lastModified = Files.getLastModifiedTime(p).toInstant();
                } catch (IOException e) {
                    // Fallback to default
                }
            }

            SupportedLanguage language = languageDetector.detect(relPathStr);

            ScanCandidate candidate = new ScanCandidate(
                relPathStr,
                p.toAbsolutePath().toString(),
                size,
                lastModified,
                "FILE",
                language
            );

            if (fileFilter.accept(candidate)) {
                candidates.add(candidate);
            }
        }

        return candidates;
    }
}
