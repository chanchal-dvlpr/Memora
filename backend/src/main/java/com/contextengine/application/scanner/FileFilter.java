package com.contextengine.application.scanner;

import java.util.Objects;

/**
 * Filter responsible for validating discovered file candidates against size limits
 * and binary/archive extension blacklists to protect vector index density.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * </p>
 */
public class FileFilter {

    /**
     * Evaluates whether the given candidate file should be included in parsing pipelines.
     *
     * @param candidate scan candidate
     * @return true if candidate is accepted, false if filtered out
     */
    public boolean accept(ScanCandidate candidate) {
        Objects.requireNonNull(candidate, "ScanCandidate must not be null");

        // Directories are skipped by default from token indexing
        if ("DIR".equals(candidate.nodeType())) {
            return false;
        }

        // Enforce maximum file size constraint
        if (candidate.size() > ScannerConstants.MAX_FILE_SIZE_BYTES) {
            return false;
        }

        // Filter out binary/archive file extensions
        String extension = getFileExtension(candidate.relativePath());
        if (ScannerConstants.DEFAULT_IGNORED_EXTENSIONS.contains(extension.toLowerCase())) {
            return false;
        }

        return true;
    }

    private String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }
}
