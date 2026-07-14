package com.contextengine.application.knowledge.search;

import com.contextengine.application.knowledge.graph.GraphNode;
import java.util.*;

/**
 * Traverses and queries KnowledgeGraph nodes using strategy filters, validation, and caching.
 */
public class SearchEngineImpl implements SearchEngine {

    private final List<SearchAlgorithm> algorithms = List.of(
        new ExactSearchStrategy(),
        new PrefixSearchStrategy(),
        new SubstringSearchStrategy(),
        new PathSearchStrategy(),
        new SymbolSearchStrategy(),
        new DependencySearchStrategy()
    );

    private final IncrementalSearchIndex indexCache = new IncrementalSearchIndex();

    @Override
    public SearchResult search(SearchContext context) {
        Objects.requireNonNull(context, "context must not be null");
        long start = System.currentTimeMillis();

        SearchStatistics stats = new SearchStatistics();
        SearchQuery query = context.query();
        SearchConfiguration config = context.configuration();

        // 1. Cache hit check
        if (context.isIncremental() && indexCache.isUnchanged(context.graph().projectId(), context.structuralHash())) {
            SearchResult cached = indexCache.getCachedResult(context.graph().projectId());
            if (cached != null) {
                return cached;
            }
        }

        List<SearchHit> hits = new ArrayList<>(config.maximumResults());
        int exactMatches = 0;
        int prefixMatches = 0;
        int substringMatches = 0;
        int pathMatches = 0;
        int symbolMatches = 0;
        int dependencyMatches = 0;

        List<SearchHit> tempNodeHits = new ArrayList<>();

        String term = query.term();
        if (term != null && !term.trim().isEmpty()) {
            for (GraphNode node : context.graph().nodes()) {
                stats.incrementEntitiesScanned(1);

                if (hits.size() >= config.maximumResults()) {
                    break;
                }

                // Run strategies sequentially. Stop on first match to avoid duplicates on the same node.
                boolean matched = false;
                for (SearchAlgorithm algo : algorithms) {
                    tempNodeHits.clear();
                    if (algo.match(node, term, config, tempNodeHits)) {
                        for (SearchHit hit : tempNodeHits) {
                            hits.add(hit);
                            stats.incrementEntitiesMatched(1);
                            
                            // Update counts
                            switch (hit.matchType()) {
                                case EXACT:
                                    exactMatches++;
                                    stats.incrementExactMatches(1);
                                    break;
                                case PREFIX:
                                    prefixMatches++;
                                    stats.incrementPrefixMatches(1);
                                    break;
                                case SUBSTRING:
                                    substringMatches++;
                                    stats.incrementSubstringMatches(1);
                                    break;
                                case PATH:
                                    pathMatches++;
                                    stats.incrementPathMatches(1);
                                    break;
                                case SYMBOL:
                                    symbolMatches++;
                                    stats.incrementSymbolMatches(1);
                                    break;
                                case DEPENDENCY:
                                    dependencyMatches++;
                                    stats.incrementDependencyMatches(1);
                                    break;
                            }
                        }
                        matched = true;
                        break;
                    }
                }

                if (!matched) {
                    stats.incrementSkippedEntities(1);
                }
            }
        }

        int finalExact = exactMatches;
        int finalPrefix = prefixMatches;
        int finalSubstring = substringMatches;

        for (SearchHit hit : hits) {
            if (hit.matchType() == SearchMatchType.PATH) {
                String path = hit.entityId().replace("file:", "").replace("dir:", "");
                String q = config.caseSensitive() ? term : term.toLowerCase();
                String src = config.caseSensitive() ? path : path.toLowerCase();
                if (src.equals(q)) {
                    finalExact++;
                } else if (src.startsWith(q)) {
                    finalPrefix++;
                } else {
                    finalSubstring++;
                }
            }
        }

        SearchSummary summary = new SearchSummary(
            hits.size(),
            finalExact,
            finalPrefix,
            finalSubstring,
            symbolMatches,
            dependencyMatches
        );

        // 2. Run Validation
        long valStart = System.currentTimeMillis();
        SearchValidator validator = new SearchValidator();
        SearchValidationResult valResult = validator.validate(hits, summary, config);
        stats.setValidationDuration(System.currentTimeMillis() - valStart);

        if (!valResult.isValid()) {
            stats.incrementWarnings(valResult.errors().size());
        }

        stats.setSearchDuration(System.currentTimeMillis() - start);

        SearchResult result = new SearchResult(hits, summary, stats);

        // Cache the result
        indexCache.cacheResult(context.graph().projectId(), context.structuralHash(), result);

        return result;
    }
}
