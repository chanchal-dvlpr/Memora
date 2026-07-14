package com.contextengine.application.knowledge.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates relational integrity and metadata boundaries of SearchResult elements.
 */
public class SearchValidator {

    /**
     * Runs schema validations.
     *
     * @param hits   search hits list
     * @param summary search summary counts
     * @param config  search configuration
     * @return validation result
     */
    public SearchValidationResult validate(List<SearchHit> hits, SearchSummary summary, SearchConfiguration config) {
        List<String> errors = new ArrayList<>();

        if (hits == null) {
            errors.add("hits list is null");
            return new SearchValidationResult(false, errors);
        }
        if (summary == null) {
            errors.add("summary is null");
            return new SearchValidationResult(false, errors);
        }

        Set<String> seenIds = new HashSet<>();
        int exact = 0;
        int prefix = 0;
        int substring = 0;
        int path = 0;
        int symbol = 0;
        int dependency = 0;

        for (int i = 0; i < hits.size(); i++) {
            SearchHit hit = hits.get(i);
            if (hit == null) {
                errors.add("SearchHit at index " + i + " is null");
                continue;
            }

            if (hit.entityId() == null || hit.entityId().isBlank()) {
                errors.add("SearchHit at index " + i + " has null/blank entityId");
            }
            if (hit.entityType() == null || hit.entityType().isBlank()) {
                errors.add("SearchHit at index " + i + " has null/blank entityType");
            }
            if (hit.matchType() == null) {
                errors.add("SearchHit at index " + i + " has null matchType");
            }

            if (hit.entityId() != null && !seenIds.add(hit.entityId())) {
                errors.add("Duplicate search hit entityId: " + hit.entityId());
            }

            if (config.includeMetadata() && hit.metadata() == null) {
                errors.add("SearchHit at index " + i + " has null metadata while includeMetadata is enabled");
            }

            if (hit.matchType() != null) {
                switch (hit.matchType()) {
                    case EXACT: exact++; break;
                    case PREFIX: prefix++; break;
                    case SUBSTRING: substring++; break;
                    case PATH: path++; break;
                    case SYMBOL: symbol++; break;
                    case DEPENDENCY: dependency++; break;
                }
            }
        }

        // Validate inconsistent summaries
        int calculatedMatches = exact + prefix + substring + path + symbol + dependency;
        if (calculatedMatches != summary.totalHits()) {
            errors.add("Inconsistent totalHits: expected " + calculatedMatches + " but summary claims " + summary.totalHits());
        }

        int sumOfSummary = summary.exactMatches() + summary.prefixMatches() + summary.substringMatches() + summary.symbolMatches() + summary.dependencyMatches();
        if (sumOfSummary != summary.totalHits()) {
            errors.add("Inconsistent summary sum: sum of individual matches " + sumOfSummary + " does not equal totalHits " + summary.totalHits());
        }

        return new SearchValidationResult(errors.isEmpty(), errors);
    }
}
