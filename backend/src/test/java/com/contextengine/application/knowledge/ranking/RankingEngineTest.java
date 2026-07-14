package com.contextengine.application.knowledge.ranking;

import com.contextengine.application.knowledge.retrieval.RetrievalResult;
import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.entity.KnowledgeRelationship;
import com.contextengine.domain.valueobject.Metadata;
import com.contextengine.domain.valueobject.NodeId;
import com.contextengine.domain.valueobject.RelationshipId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests verifying relevance sorting, exponential decays, and proximity scores in the RankingEngine.
 */
class RankingEngineTest {

    private RankingEngine rankingEngine;

    @BeforeEach
    void setUp() {
        rankingEngine = new RankingEngine();
    }

    @Test
    void testRankNodesCorrectOrdering() {
        NodeId activeId = NodeId.generate();
        NodeId secondId = NodeId.generate();

        KnowledgeNode activeFileNode = new KnowledgeNode(activeId, "FILE", new Metadata(Map.of(
            "urn", "urn:ce:node:test:file:active",
            "filePath", "src/App.java",
            "name", "App.java",
            "tokens", "100"
        )));

        KnowledgeNode otherNode = new KnowledgeNode(secondId, "BUG", new Metadata(Map.of(
            "urn", "urn:ce:node:test:bug:other",
            "name", "Crash bug",
            "severity", "CRITICAL"
        )));

        // Create relationship from activeFileNode to otherNode (distance 1)
        KnowledgeRelationship relationship = new KnowledgeRelationship(
            new RelationshipId(UUID.randomUUID()),
            activeId,
            secondId,
            "AFFECTS",
            new com.contextengine.domain.valueobject.GraphWeight(1.0)
        );

        RetrievalResult retrievalResult = new RetrievalResult(
            Arrays.asList(activeFileNode, otherNode),
            Collections.singletonList(relationship)
        );

        List<ContextRankedResult> ranked = rankingEngine.rank(retrievalResult, "crash", "src/App.java");

        // The activeFileNode has proximity = 1.0 (distance 0)
        // The otherNode has queryFocus matching term 'crash' and severity=CRITICAL, but distance=1 (proximity = 0.8)
        assertThat(ranked).hasSize(2);
        // The bug node (secondId) should be first because it matches the query focus 'crash' and has CRITICAL severity
        assertThat(ranked.get(0).node().id()).isEqualTo(secondId);
        assertThat(ranked.get(1).node().id()).isEqualTo(activeId);
    }
}
