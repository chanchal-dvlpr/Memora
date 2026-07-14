package com.contextengine.application.knowledge.snapshot;

/**
 * Extended statistics metrics tracked during snapshot comparison execution runs.
 */
public class SnapshotComparisonStatistics {
    private int entitiesCompared;
    private int differencesFound;
    private int additions;
    private int removals;
    private int modifications;
    private int renames;
    private int moves;
    private int unchanged;
    private long validationDuration;
    private long comparisonDuration;
    private int warnings;
    private int skippedEntities;

    public int entitiesCompared() {
        return entitiesCompared;
    }

    public void incrementEntitiesCompared(int count) {
        this.entitiesCompared += count;
    }

    public int differencesFound() {
        return differencesFound;
    }

    public void incrementDifferencesFound(int count) {
        this.differencesFound += count;
    }

    public int additions() {
        return additions;
    }

    public void incrementAdditions(int count) {
        this.additions += count;
    }

    public int removals() {
        return removals;
    }

    public void incrementRemovals(int count) {
        this.removals += count;
    }

    public int modifications() {
        return modifications;
    }

    public void incrementModifications(int count) {
        this.modifications += count;
    }

    public int renames() {
        return renames;
    }

    public void incrementRenames(int count) {
        this.renames += count;
    }

    public int moves() {
        return moves;
    }

    public void incrementMoves(int count) {
        this.moves += count;
    }

    public int unchanged() {
        return unchanged;
    }

    public void incrementUnchanged(int count) {
        this.unchanged += count;
    }

    public long validationDuration() {
        return validationDuration;
    }

    public void setValidationDuration(long duration) {
        this.validationDuration = duration;
    }

    public long comparisonDuration() {
        return comparisonDuration;
    }

    public void setComparisonDuration(long duration) {
        this.comparisonDuration = duration;
    }

    public int warnings() {
        return warnings;
    }

    public void incrementWarnings(int count) {
        this.warnings += count;
    }

    public int skippedEntities() {
        return skippedEntities;
    }

    public void incrementSkippedEntities(int count) {
        this.skippedEntities += count;
    }
}
