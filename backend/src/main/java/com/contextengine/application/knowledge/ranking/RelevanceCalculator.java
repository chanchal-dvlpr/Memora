package com.contextengine.application.knowledge.ranking;

import com.contextengine.domain.entity.KnowledgeNode;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Computes priority relevance scores for knowledge nodes based on the architecture-defined formula.
 * <p>
 * Bounded Context: Knowledge Graph / Context Assembly
 * Architecture Reference: Functional Requirement FR-014 (Context Generation Engine) Section 9 (Relevance Ranking Formula)
 * Responsibility: Calculates scores utilizing temporal decay, shortest path proximity, severity weights, and query focus term overlaps.
 * Dependencies: {@link KnowledgeNode}.
 * Future Usage: Integration with neural or vector-based similarity scoring models.
 * </p>
 */
public final class RelevanceCalculator {

    private static final double DECAY_CONSTANT = 0.00001; // Decays over time (half-life of ~19 hours)

    private RelevanceCalculator() {
        // Prevent instantiation
    }

    /**
     * Computes the relevance score for a given node.
     * Formula: Score = (0.35 * TemporalRecency) + (0.30 * DependencyProximity) + (0.20 * SeverityWeight) + (0.15 * QueryFocus)
     *
     * @param node the knowledge node to calculate score for
     * @param distances mapping of node IDs to shortest-path distances from the active node
     * @param queryTerms lowercase tokenized search terms
     * @param currentEpochMs the current system timestamp in epoch milliseconds
     * @return the calculated double score between 0.0 and 1.0
     */
    public static double calculateScore(
        KnowledgeNode node,
        Map<com.contextengine.domain.valueobject.NodeId, Integer> distances,
        Set<String> queryTerms,
        long currentEpochMs
    ) {
        Objects.requireNonNull(node, "Node must not be null");
        Objects.requireNonNull(distances, "Distances map must not be null");
        Objects.requireNonNull(queryTerms, "Query terms must not be null");

        // 1. Temporal Recency (decay-based)
        double recency = 0.5; // default fallback value
        String lastModAttr = node.attributes().get("lastModified");
        if (lastModAttr != null) {
            try {
                long lastMod = Long.parseLong(lastModAttr);
                long diffSeconds = (currentEpochMs - lastMod) / 1000;
                if (diffSeconds < 0) {
                    diffSeconds = 0;
                }
                recency = Math.exp(-DECAY_CONSTANT * diffSeconds);
            } catch (NumberFormatException ignored) {}
        }

        // 2. Dependency Proximity (shortest-path distance)
        double proximity = 0.2; // default fallback value (not connected or far)
        Integer distance = distances.get(node.id());
        if (distance != null) {
            if (distance == 0) {
                proximity = 1.0;
            } else if (distance == 1) {
                proximity = 0.8;
            } else if (distance == 2) {
                proximity = 0.5;
            } else {
                proximity = 0.3;
            }
        }

        // 3. Severity Weight (defects or constraints check)
        double severity = 0.0;
        String type = node.type();
        if (type.equalsIgnoreCase("BUG") || type.equalsIgnoreCase("CONSTRAINT")) {
            String sevAttr = node.attributes().get("severity");
            if (sevAttr != null) {
                if (sevAttr.equalsIgnoreCase("CRITICAL") || sevAttr.equalsIgnoreCase("HARD_BLOCK")) {
                    severity = 1.0;
                } else if (sevAttr.equalsIgnoreCase("HIGH") || sevAttr.equalsIgnoreCase("MAJOR")) {
                    severity = 0.7;
                } else if (sevAttr.equalsIgnoreCase("MEDIUM") || sevAttr.equalsIgnoreCase("MINOR")) {
                    severity = 0.4;
                } else {
                    severity = 0.2;
                }
            }
        }

        // 4. Query Focus (term intersection ratio)
        double queryFocus = 0.0;
        if (!queryTerms.isEmpty()) {
            String qname = node.attributes().get("qualifiedName");
            if (qname == null) {
                qname = node.attributes().get("name");
            }
            final String nameToMatch = qname != null ? qname.toLowerCase() : "";
            final String docstringToMatch = node.attributes().get("docstring") != null ? node.attributes().get("docstring").toLowerCase() : "";
            final String typeToMatch = node.type() != null ? node.type().toLowerCase() : "";

            long matches = queryTerms.stream()
                .filter(term -> nameToMatch.contains(term) ||
                                 docstringToMatch.contains(term) ||
                                 typeToMatch.contains(term))
                .count();
            queryFocus = (double) matches / queryTerms.size();
        }

        // Compute weighted sum
        return (0.35 * recency) + (0.30 * proximity) + (0.20 * severity) + (0.15 * queryFocus);
    }
}
