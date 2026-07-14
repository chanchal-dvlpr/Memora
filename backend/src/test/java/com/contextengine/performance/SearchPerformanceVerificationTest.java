package com.contextengine.performance;

import com.contextengine.application.knowledge.graph.*;
import com.contextengine.application.knowledge.search.*;
import com.contextengine.test.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance and production compliance test suite for Search & Retrieval.
 */
class SearchPerformanceVerificationTest extends BaseIntegrationTest {

    @Autowired
    private SearchEngine searchEngine;

    private KnowledgeGraph scaleGraph;

    @BeforeEach
    void setUpScaleGraph() {
        KnowledgeGraphConfiguration graphConfig = new KnowledgeGraphConfiguration(false, true);
        scaleGraph = new KnowledgeGraph("scale-project", graphConfig);

        // 1. Add 4,000 files
        for (int i = 0; i < 4000; i++) {
            Map<String, Object> props = new HashMap<>();
            props.put("absolutePath", "/workspace/src/File" + i + ".java");
            props.put("language", "JAVA");
            props.put("structuralHash", "hash-file-" + i);
            GraphNode fileNode = new GraphNode("file:src/File" + i + ".java", GraphNode.Type.FILE, "File" + i + ".java", props);
            scaleGraph.addNode(fileNode);
        }

        // 2. Add 800 symbols
        for (int i = 0; i < 800; i++) {
            Map<String, Object> props = new HashMap<>();
            props.put("filePath", "src/File" + (i % 4000) + ".java");
            props.put("kind", "CLASS");
            GraphNode symNode = new GraphNode("symbol:src/File" + (i % 4000) + ".java:Symbol" + i, GraphNode.Type.SYMBOL, "Symbol" + i, props);
            scaleGraph.addNode(symNode);
        }

        // 3. Add 200 dependencies
        for (int i = 0; i < 200; i++) {
            Map<String, Object> props = new HashMap<>();
            props.put("version", "1." + i + ".0");
            GraphNode depNode = new GraphNode("dep:dependency-" + i, GraphNode.Type.DEPENDENCY, "dependency-" + i, props);
            scaleGraph.addNode(depNode);
        }
    }

    @Test
    void verifyPerformanceAndThroughput() throws InterruptedException {
        // Run once to warm up
        SearchQuery warmupQuery = new SearchQuery("File100", Collections.emptyMap());
        SearchConfiguration config = new SearchConfiguration();
        SearchContext warmupContext = new SearchContext(scaleGraph, warmupQuery, config);
        searchEngine.search(warmupContext);

        // 1. Measure typical search & validation latency
        long start = System.nanoTime();
        SearchResult result = searchEngine.search(warmupContext);
        long end = System.nanoTime();

        double searchDurationMs = (end - start) / 1_000_000.0;
        System.out.printf("[PERFORMANCE] Search + Validation latency for 5,000 nodes: %.2f ms\n", searchDurationMs);

        // NFR checks
        assertThat(searchDurationMs).isLessThan(200.0);
        assertThat(result.statistics().validationDuration()).isLessThan(50L);

        // 2. Measure cache hit reuse latency
        SearchContext cachedContext = new SearchContext(scaleGraph, warmupQuery, config, "structural-hash-1", true);
        searchEngine.search(cachedContext); // populate cache

        long cacheStart = System.nanoTime();
        SearchResult cachedResult = searchEngine.search(cachedContext);
        long cacheEnd = System.nanoTime();

        double cacheDurationMs = (cacheEnd - cacheStart) / 1_000_000.0;
        System.out.printf("[PERFORMANCE] Cache hit latency: %.2f ms\n", cacheDurationMs);
        assertThat(cacheDurationMs).isLessThan(25.0);

        // 3. Concurrent Search executions (Thread Safety)
        int threadsCount = 30;
        ExecutorService executor = Executors.newFixedThreadPool(threadsCount);
        CountDownLatch latch = new CountDownLatch(threadsCount);
        List<SearchResult> results = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadsCount; i++) {
            final String qTerm = "File" + (i * 10);
            executor.submit(() -> {
                try {
                    SearchQuery q = new SearchQuery(qTerm, Collections.emptyMap());
                    SearchContext ctx = new SearchContext(scaleGraph, q, config);
                    results.add(searchEngine.search(ctx));
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        assertTrue(finished);

        assertEquals(threadsCount, results.size());
        for (SearchResult r : results) {
            assertNotNull(r);
            assertTrue(r.hits().size() > 0);
        }
    }

    @Test
    void verifyFunctionalAndBoundaryCases() {
        // 1. Empty Knowledge Graph
        KnowledgeGraph emptyGraph = new KnowledgeGraph("empty-project", new KnowledgeGraphConfiguration(false, true));
        SearchQuery query = new SearchQuery("File", Collections.emptyMap());
        SearchConfiguration config = new SearchConfiguration();
        SearchResult emptyResult = searchEngine.search(new SearchContext(emptyGraph, query, config));
        assertEquals(0, emptyResult.summary().totalHits());

        // 2. Empty Query
        SearchQuery emptyQuery = new SearchQuery("", Collections.emptyMap());
        SearchResult emptyQueryResult = searchEngine.search(new SearchContext(scaleGraph, emptyQuery, config));
        assertEquals(0, emptyQueryResult.summary().totalHits());

        // 3. Maximum results limit
        config.setMaximumResults(10);
        SearchResult limitResult = searchEngine.search(new SearchContext(scaleGraph, query, config));
        assertEquals(10, limitResult.hits().size());
        assertEquals(10, limitResult.summary().totalHits());

        // 4. Case sensitivity
        SearchQuery lowerQuery = new SearchQuery("file3999", Collections.emptyMap());
        SearchConfiguration caseSensitiveConfig = new SearchConfiguration();
        caseSensitiveConfig.setCaseSensitive(true);

        SearchResult caseSensitiveResult = searchEngine.search(new SearchContext(scaleGraph, lowerQuery, caseSensitiveConfig));
        assertEquals(0, caseSensitiveResult.summary().totalHits()); // "File3999.java" != "file3999" case-sensitive

        caseSensitiveConfig.setCaseSensitive(false);
        SearchResult caseInsensitiveResult = searchEngine.search(new SearchContext(scaleGraph, lowerQuery, caseSensitiveConfig));
        assertEquals(1, caseInsensitiveResult.summary().totalHits());
    }

    @Test
    void verifyStrictAndLaxValidationModes() {
        SearchValidator validator = new SearchValidator();
        SearchConfiguration config = new SearchConfiguration();

        // Lax validation test: return valid / invalid
        SearchSummary summary = new SearchSummary(10, 10, 0, 0, 0, 0);
        List<SearchHit> validHits = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            validHits.add(new SearchHit("file:" + i, "FILE", "File" + i, "src/File" + i, SearchMatchType.EXACT, Collections.emptyMap()));
        }
        SearchValidationResult validResult = validator.validate(validHits, summary, config);
        assertTrue(validResult.isValid());

        // STRICT mode validation errors checking
        SearchSummary badSummary = new SearchSummary(5, 10, 0, 0, 0, 0);
        SearchValidationResult invalidResult = validator.validate(validHits, badSummary, config);
        assertFalse(invalidResult.isValid());
        assertTrue(invalidResult.errors().size() > 0);
    }
}
