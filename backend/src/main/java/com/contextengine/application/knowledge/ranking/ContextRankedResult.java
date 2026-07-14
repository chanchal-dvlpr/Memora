package com.contextengine.application.knowledge.ranking;

import com.contextengine.domain.entity.KnowledgeNode;
import java.util.Objects;

/**
 * Represents a retrieved knowledge node paired with its calculated relevance score.
 * Implements {@link Comparable} to maintain descending priority ordering.
 * <p>
 * Bounded Context: Knowledge Graph / Context Assembly
 * Architecture Reference: Functional Requirement FR-014 (Context Generation Engine) Section 8 (Lifecycle state: RANKED)
 * Responsibility: Value object for storing prioritization results.
 * Dependencies: {@link KnowledgeNode}.
 * Future Usage: Feeds into the token budget truncation algorithms.
 * </p>
 */
public record ContextRankedResult(
    KnowledgeNode node,
    double score
) implements Comparable<ContextRankedResult> {

    /**
     * Constructs a ContextRankedResult.
     *
     * @param node the ranked knowledge node
     * @param score the priority relevance score
     * @throws NullPointerException if node is null
     */
    public ContextRankedResult {
        Objects.requireNonNull(node, "Node must not be null");
    }

    @Override
    public int compareTo(ContextRankedResult other) {
        // Sort descending by score
        return Double.compare(other.score, this.score);
    }
}
