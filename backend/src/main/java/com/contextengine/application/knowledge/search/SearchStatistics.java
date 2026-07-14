package com.contextengine.application.knowledge.search;

/**
 * Tracks traversal and matching latencies for query execution.
 */
public class SearchStatistics {
    private int entitiesScanned;
    private int entitiesMatched;
    private int exactMatches;
    private int prefixMatches;
    private int substringMatches;
    private int pathMatches;
    private int symbolMatches;
    private int dependencyMatches;
    private long validationDuration;
    private long searchDuration;
    private int warnings;
    private int skippedEntities;

    public int entitiesScanned() {
        return entitiesScanned;
    }

    public void incrementEntitiesScanned(int count) {
        this.entitiesScanned += count;
    }

    public int entitiesMatched() {
        return entitiesMatched;
    }

    public void incrementEntitiesMatched(int count) {
        this.entitiesMatched += count;
    }

    public int exactMatches() {
        return exactMatches;
    }

    public void incrementExactMatches(int count) {
        this.exactMatches += count;
    }

    public int prefixMatches() {
        return prefixMatches;
    }

    public void incrementPrefixMatches(int count) {
        this.prefixMatches += count;
    }

    public int substringMatches() {
        return substringMatches;
    }

    public void incrementSubstringMatches(int count) {
        this.substringMatches += count;
    }

    public int pathMatches() {
        return pathMatches;
    }

    public void incrementPathMatches(int count) {
        this.pathMatches += count;
    }

    public int symbolMatches() {
        return symbolMatches;
    }

    public void incrementSymbolMatches(int count) {
        this.symbolMatches += count;
    }

    public int dependencyMatches() {
        return dependencyMatches;
    }

    public void incrementDependencyMatches(int count) {
        this.dependencyMatches += count;
    }

    public long validationDuration() {
        return validationDuration;
    }

    public void setValidationDuration(long duration) {
        this.validationDuration = duration;
    }

    public long searchDuration() {
        return searchDuration;
    }

    public void setSearchDuration(long duration) {
        this.searchDuration = duration;
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
