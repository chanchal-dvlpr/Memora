package com.contextengine.application.knowledge.search;

/**
 * Summarizes comparison counts by search matching logic strength.
 */
public record SearchSummary(
    int totalHits,
    int exactMatches,
    int prefixMatches,
    int substringMatches,
    int symbolMatches,
    int dependencyMatches
) {}
