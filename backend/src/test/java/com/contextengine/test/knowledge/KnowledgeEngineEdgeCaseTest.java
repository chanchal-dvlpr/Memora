package com.contextengine.test.knowledge;

import com.contextengine.application.knowledge.engine.*;
import com.contextengine.application.knowledge.graph.GraphNode;
import com.contextengine.application.knowledge.graph.GraphRelationship;
import com.contextengine.application.knowledge.graph.KnowledgeGraph;
import com.contextengine.application.knowledge.search.*;
import com.contextengine.application.scanner.ScanCandidate;
import com.contextengine.application.scanner.SupportedLanguage;
import com.contextengine.test.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge case test suite covering unicode paths, long file names, and circular graphs.
 */
class KnowledgeEngineEdgeCaseTest extends BaseIntegrationTest {

    @Autowired
    private KnowledgeEngine knowledgeEngine;

    @Autowired
    private SearchEngine searchEngine;

    @Test
    void testUnicodeAndLongFilenamePaths() {
        String projectId = java.util.UUID.randomUUID().toString();
        String unicodePath = "src/üñîçødê/VeryLongFilename" + "A".repeat(100) + ".java";

        Collection<ScanCandidate> candidates = List.of(
            new ScanCandidate(unicodePath, "/workspace/" + unicodePath, 500L, Instant.now(), "FILE", SupportedLanguage.JAVA)
        );

        KnowledgeEngineContext context = TestContextFactory.createEngineContext(
            projectId, candidates, Collections.emptyList(), Collections.emptyList()
        );

        KnowledgeEngineResult result = knowledgeEngine.process(context);
        assertNotNull(result);
        assertEquals("COMPLETED", result.processingStatus());

        SearchQuery query = new SearchQuery("üñîçødê", Collections.emptyMap());
        SearchResult searchResult = searchEngine.search(new SearchContext(result.graph(), query, new SearchConfiguration()));
        assertTrue(searchResult.hits().size() > 0);
    }

    @Test
    void testCircularGraphReferences() {
        String projectId = java.util.UUID.randomUUID().toString();
        KnowledgeGraph graph = TestKnowledgeGraphFactory.createEmptyGraph(projectId);

        GraphNode nodeA = new GraphNode("file:src/A.java", GraphNode.Type.FILE, "A.java", new HashMap<>());
        GraphNode nodeB = new GraphNode("file:src/B.java", GraphNode.Type.FILE, "B.java", new HashMap<>());
        graph.addNode(nodeA);
        graph.addNode(nodeB);

        graph.addRelationship(new GraphRelationship("file:src/A.java", "file:src/B.java", GraphRelationship.Type.REFERENCES, Collections.emptyMap()));
        graph.addRelationship(new GraphRelationship("file:src/B.java", "file:src/A.java", GraphRelationship.Type.REFERENCES, Collections.emptyMap()));

        SearchQuery query = new SearchQuery("A.java", Collections.emptyMap());
        SearchResult result = searchEngine.search(new SearchContext(graph, query, new SearchConfiguration()));
        assertNotNull(result);
        assertEquals(1, result.summary().totalHits());
    }
}
