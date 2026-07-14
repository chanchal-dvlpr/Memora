package com.contextengine.application.knowledge.ranking;

import com.contextengine.application.knowledge.retrieval.RetrievalResult;
import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.entity.KnowledgeRelationship;
import com.contextengine.domain.valueobject.NodeId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Prioritizes retrieved candidates using domain-specific heuristics and exponential decays.
 * <p>
 * Bounded Context: Knowledge Graph / Context Assembly
 * Architecture Reference: Functional Requirement FR-014 (Context Generation Engine) Section 8 (Lifecycle state: RANKED)
 * Responsibility: Performs shortest path BFS calculations and computes relevance priority rankings.
 * Dependencies: {@link RetrievalResult}, {@link RelevanceCalculator}, {@link ContextRankedResult}.
 * Future Usage: Integration with active user activity weights and editor selection highlights.
 * </p>
 */
public class RankingEngine {

    /**
     * Constructs a RankingEngine.
     */
    public RankingEngine() {
        // Default constructor
    }

    /**
     * Prioritizes and orders retrieved nodes descending by computed priority score.
     *
     * @param retrievalResult raw retrieved nodes and edges
     * @param contextQuery search query string
     * @param activeFilePath optional path to the active file being edited
     * @return sorted list of ranked nodes with their scores
     */
    public List<ContextRankedResult> rank(
        RetrievalResult retrievalResult,
        String contextQuery,
        String activeFilePath
    ) {
        Objects.requireNonNull(retrievalResult, "RetrievalResult must not be null");

        // 1. Compute shortest path distances from active file seed node
        Map<NodeId, Integer> distances = computeShortestPaths(
            retrievalResult.nodes(),
            retrievalResult.relationships(),
            activeFilePath
        );

        // 2. Tokenize search query keywords
        Set<String> queryTerms = new HashSet<>();
        if (contextQuery != null && !contextQuery.isBlank()) {
            String[] terms = contextQuery.toLowerCase().split("\\s+");
            for (String term : terms) {
                if (!term.isBlank()) {
                    queryTerms.add(term);
                }
            }
        }

        // 3. Compute scores and build ranked list
        long currentEpochMs = System.currentTimeMillis();
        List<ContextRankedResult> rankedResults = new ArrayList<>();
        for (KnowledgeNode node : retrievalResult.nodes()) {
            double score = RelevanceCalculator.calculateScore(node, distances, queryTerms, currentEpochMs);
            rankedResults.add(new ContextRankedResult(node, score));
        }

        // 4. Sort descending (using Comparable implemented in ContextRankedResult)
        Collections.sort(rankedResults);

        return rankedResults;
    }

    private Map<NodeId, Integer> computeShortestPaths(
        Collection<KnowledgeNode> nodes,
        Collection<KnowledgeRelationship> relationships,
        String activeFilePath
    ) {
        Map<NodeId, Integer> distances = new HashMap<>();
        if (activeFilePath == null || activeFilePath.isBlank()) {
            return distances;
        }

        KnowledgeNode startNode = nodes.stream()
            .filter(n -> {
                if (!n.type().equalsIgnoreCase("FILE")) {
                    return false;
                }
                String qname = n.attributes().get("qualifiedName");
                if (qname == null) {
                    qname = n.attributes().get("name");
                }
                return (qname != null && qname.endsWith(activeFilePath)) ||
                       (n.attributes().get("filePath") != null && n.attributes().get("filePath").equals(activeFilePath));
            })
            .findFirst()
            .orElse(null);

        if (startNode == null) {
            return distances;
        }

        Queue<NodeId> queue = new LinkedList<>();
        queue.add(startNode.id());
        distances.put(startNode.id(), 0);

        // Build adjacency lists from relationships
        Map<NodeId, List<NodeId>> adj = new HashMap<>();
        for (KnowledgeNode node : nodes) {
            adj.put(node.id(), new ArrayList<>());
        }
        for (KnowledgeRelationship rel : relationships) {
            if (adj.containsKey(rel.sourceNodeId()) && adj.containsKey(rel.targetNodeId())) {
                adj.get(rel.sourceNodeId()).add(rel.targetNodeId());
                adj.get(rel.targetNodeId()).add(rel.sourceNodeId());
            }
        }

        while (!queue.isEmpty()) {
            NodeId curr = queue.poll();
            int d = distances.get(curr);
            for (NodeId neighbor : adj.getOrDefault(curr, Collections.emptyList())) {
                if (!distances.containsKey(neighbor)) {
                    distances.put(neighbor, d + 1);
                    queue.add(neighbor);
                }
            }
        }
        return distances;
    }
}
