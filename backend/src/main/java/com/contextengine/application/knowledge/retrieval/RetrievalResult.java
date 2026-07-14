package com.contextengine.application.knowledge.retrieval;

import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.entity.KnowledgeRelationship;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Holds the unranked nodes and relationships fetched by the Retrieval Engine.
 * <p>
 * Bounded Context: Knowledge Graph / Context Assembly
 * Architecture Reference: Functional Requirement FR-014 (Context Generation Engine) Section 8 (Lifecycle state: COLLECTED)
 * Responsibility: Value object encapsulating raw retrieved codebase entities and their connections.
 * Dependencies: {@link KnowledgeNode}, {@link KnowledgeRelationship}.
 * Future Usage: Feeds into the RelevanceScoringEngine / RankingEngine.
 * </p>
 */
public record RetrievalResult(
    Collection<KnowledgeNode> nodes,
    Collection<KnowledgeRelationship> relationships
) {
    /**
     * Constructs a RetrievalResult.
     *
     * @param nodes retrieved knowledge nodes
     * @param relationships retrieved knowledge relationships
     * @throws NullPointerException if nodes or relationships is null
     */
    public RetrievalResult {
        Objects.requireNonNull(nodes, "Nodes must not be null");
        Objects.requireNonNull(relationships, "Relationships must not be null");
        nodes = Collections.unmodifiableCollection(nodes);
        relationships = Collections.unmodifiableCollection(relationships);
    }
}
