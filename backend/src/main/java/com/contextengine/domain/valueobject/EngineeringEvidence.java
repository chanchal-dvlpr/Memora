package com.contextengine.domain.valueobject;

import com.contextengine.domain.model.ValueObject;
import java.util.Objects;

/**
 * Bundles the physical evidence proving a semantic relationship or validating an assumption.
 * Contains a file Path, a line number range, and the matching file content hash.
 */
public record EngineeringEvidence(
    Path filePath,
    int startLine,
    int endLine,
    Hash fileContentHash
) implements ValueObject {
    
    /**
     * Constructs EngineeringEvidence and validates the line numbers and null bounds.
     *
     * @param filePath the path to the file containing the evidence
     * @param startLine the starting line number (1-indexed, positive)
     * @param endLine the ending line number (must be >= startLine)
     * @param fileContentHash the cryptographic hash of the target file content
     * @throws NullPointerException if filePath or fileContentHash is null
     * @throws IllegalArgumentException if line ranges are invalid (startLine < 1 or endLine < startLine)
     */
    public EngineeringEvidence {
        Objects.requireNonNull(filePath, "File path must not be null");
        Objects.requireNonNull(fileContentHash, "File content hash must not be null");
        if (startLine < 1) {
            throw new IllegalArgumentException("Start line must be positive (>= 1)");
        }
        if (endLine < startLine) {
            throw new IllegalArgumentException("End line must be greater than or equal to start line");
        }
    }
}
