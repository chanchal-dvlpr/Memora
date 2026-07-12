package com.contextengine.infrastructure.search;

import com.contextengine.domain.entity.KnowledgeNode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Combines and de-duplicates raw keyword search results and semantic matches into a unified, ranked list.
 * <p>
 * Bounded Context: Search Engine
 * Related Subsystem: Search Engine Subsystem (SE-SUB)
 * </p>
 */
public class SearchCombiner {

    /**
     * Merges and ranks matching nodes based on query term occurrence and matching kind.
     *
     * @param keywordMatches matches from the inverted index
     * @param queryTerm query term
     * @return unified collection of ranked nodes
     */
    public Collection<KnowledgeNode> combineAndRank(Collection<KnowledgeNode> keywordMatches, String queryTerm) {
        Objects.requireNonNull(keywordMatches, "Keyword matches must not be null");
        Objects.requireNonNull(queryTerm, "Query term must not be null");

        String normalized = queryTerm.toLowerCase().trim();

        return keywordMatches.stream()
            .distinct()
            .sorted((n1, n2) -> {
                String name1 = n1.attributes().get("name");
                String name2 = n2.attributes().get("name");
                if (name1 != null && name2 != null) {
                    boolean exact1 = name1.toLowerCase().equals(normalized);
                    boolean exact2 = name2.toLowerCase().equals(normalized);
                    if (exact1 && !exact2) {
                        return -1;
                    }
                    if (!exact1 && exact2) {
                        return 1;
                    }
                }
                return 0;
            })
            .collect(Collectors.toList());
    }
}
