package com.contextengine.infrastructure.search;

import com.contextengine.domain.entity.KnowledgeNode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages in-memory inverted index mappings for rapid keyword-based code search.
 * <p>
 * Bounded Context: Search Engine
 * Related Subsystem: Search Engine Subsystem (SE-SUB)
 * </p>
 */
public class InvertedIndexManager {

    private final Map<String, List<KnowledgeNode>> index = new HashMap<>();

    /**
     * Indexes a collection of nodes for keyword search.
     *
     * @param nodes the nodes to index
     */
    public void indexNodes(Collection<KnowledgeNode> nodes) {
        Objects.requireNonNull(nodes, "Nodes must not be null");
        index.clear();
        for (KnowledgeNode node : nodes) {
            String name = node.attributes().get("name");
            if (name != null) {
                tokenize(name).forEach(token -> 
                    index.computeIfAbsent(token, k -> new ArrayList<>()).add(node)
                );
            }
        }
    }

    /**
     * Finds nodes matching the search term.
     *
     * @param term the query search term
     * @return collection of matching nodes
     */
    public Collection<KnowledgeNode> findMatches(String term) {
        Objects.requireNonNull(term, "Term must not be null");
        String normalized = term.toLowerCase().trim();
        return index.entrySet().stream()
            .filter(entry -> entry.getKey().contains(normalized))
            .flatMap(entry -> entry.getValue().stream())
            .collect(Collectors.toSet());
    }

    private List<String> tokenize(String value) {
        if (value == null) {
            return List.of();
        }
        return Arrays.stream(value.toLowerCase().split("[^a-zA-Z0-9]+"))
            .filter(t -> !t.isEmpty())
            .toList();
    }
}
