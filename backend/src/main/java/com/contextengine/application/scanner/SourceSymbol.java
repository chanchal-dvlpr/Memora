package com.contextengine.application.scanner;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Normalized model representing a parsed structural code symbol in the workspace.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * </p>
 */
public record SourceSymbol(
    String name,
    String kind,
    String filePath,
    int startLine,
    int endLine,
    Map<String, String> metadata
) {
    /**
     * Constructs a SourceSymbol.
     *
     * @param name unique name or identifier of the symbol
     * @param kind classification kind (e.g., CLASS, METHOD, FILE)
     * @param filePath relative or absolute path to the file containing this symbol
     * @param startLine 1-based start line coordinate
     * @param endLine 1-based end line coordinate
     * @param metadata key-value dictionary containing custom attributes
     */
    public SourceSymbol {
        Objects.requireNonNull(name, "Name must not be null");
        Objects.requireNonNull(kind, "Kind must not be null");
        Objects.requireNonNull(filePath, "FilePath must not be null");
        Objects.requireNonNull(metadata, "Metadata must not be null");

        if (startLine < 1) {
            throw new IllegalArgumentException("Start line must be at least 1");
        }
        if (endLine < startLine) {
            throw new IllegalArgumentException("End line must be at or after start line");
        }
    }

    /**
     * Gets an unmodifiable view of the metadata map.
     *
     * @return unmodifiable metadata map
     */
    public Map<String, String> metadata() {
        return Collections.unmodifiableMap(metadata);
    }
}
