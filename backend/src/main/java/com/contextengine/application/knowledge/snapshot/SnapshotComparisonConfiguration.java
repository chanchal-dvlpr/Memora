package com.contextengine.application.knowledge.snapshot;

/**
 * Configuration options regulating comparison analysis.
 */
public class SnapshotComparisonConfiguration {
    private final boolean detectRenames;
    private final boolean detectMoves;
    private final boolean compareMetadata;
    private final boolean compareHashes;
    private final boolean compareStructure;

    public SnapshotComparisonConfiguration() {
        this(true, true, true, true, true);
    }

    public SnapshotComparisonConfiguration(
        boolean detectRenames,
        boolean detectMoves,
        boolean compareMetadata,
        boolean compareHashes,
        boolean compareStructure
    ) {
        this.detectRenames = detectRenames;
        this.detectMoves = detectMoves;
        this.compareMetadata = compareMetadata;
        this.compareHashes = compareHashes;
        this.compareStructure = compareStructure;
    }

    public boolean detectRenames() {
        return detectRenames;
    }

    public boolean detectMoves() {
        return detectMoves;
    }

    public boolean compareMetadata() {
        return compareMetadata;
    }

    public boolean compareHashes() {
        return compareHashes;
    }

    public boolean compareStructure() {
        return compareStructure;
    }
}
