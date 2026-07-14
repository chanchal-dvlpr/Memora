package com.contextengine.application.knowledge.search;

import com.contextengine.application.knowledge.graph.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SearchEngineTest {

    @Autowired
    private SearchEngine searchEngine;

    private KnowledgeGraph graph;

    @BeforeEach
    void setUp() {
        KnowledgeGraphConfiguration graphConfig = new KnowledgeGraphConfiguration(false, true);
        graph = new KnowledgeGraph("test-project", graphConfig);

        // Add a FILE node
        Map<String, Object> fileProps = new HashMap<>();
        fileProps.put("absolutePath", "/workspace/src/UserController.java");
        fileProps.put("language", "JAVA");
        GraphNode fileNode = new GraphNode("file:src/UserController.java", GraphNode.Type.FILE, "UserController.java", fileProps);
        graph.addNode(fileNode);

        // Add a SYMBOL node
        Map<String, Object> symProps = new HashMap<>();
        symProps.put("filePath", "src/UserController.java");
        symProps.put("kind", "CLASS");
        GraphNode symNode = new GraphNode("symbol:src/UserController.java:UserController", GraphNode.Type.SYMBOL, "UserController", symProps);
        graph.addNode(symNode);

        // Add a DEPENDENCY node
        Map<String, Object> depProps = new HashMap<>();
        depProps.put("version", "3.0.0");
        GraphNode depNode = new GraphNode("dep:spring-core", GraphNode.Type.DEPENDENCY, "spring-core", depProps);
        graph.addNode(depNode);
    }

    @Test
    void testSpringBeanRegistration() {
        assertNotNull(searchEngine);
    }

    @Test
    void testExactSearch() {
        SearchQuery query = new SearchQuery("UserController.java", Collections.emptyMap());
        SearchConfiguration config = new SearchConfiguration();
        SearchContext context = new SearchContext(graph, query, config);

        SearchResult result = searchEngine.search(context);

        assertEquals(1, result.summary().totalHits());
        assertEquals(1, result.summary().exactMatches());
        assertEquals(SearchMatchType.EXACT, result.hits().get(0).matchType());
    }

    @Test
    void testPrefixSearch() {
        SearchQuery query = new SearchQuery("UserCon", Collections.emptyMap());
        SearchConfiguration config = new SearchConfiguration();
        SearchContext context = new SearchContext(graph, query, config);

        SearchResult result = searchEngine.search(context);

        assertEquals(2, result.summary().totalHits());
        assertEquals(1, result.summary().prefixMatches());
        assertEquals(1, result.summary().symbolMatches());
    }

    @Test
    void testSubstringSearch() {
        SearchQuery query = new SearchQuery("Controll", Collections.emptyMap());
        SearchConfiguration config = new SearchConfiguration();
        SearchContext context = new SearchContext(graph, query, config);

        SearchResult result = searchEngine.search(context);

        assertEquals(2, result.summary().totalHits());
        assertEquals(1, result.summary().substringMatches());
    }

    @Test
    void testPathSearch() {
        SearchQuery query = new SearchQuery("src/UserController", Collections.emptyMap());
        SearchConfiguration config = new SearchConfiguration();
        SearchContext context = new SearchContext(graph, query, config);

        SearchResult result = searchEngine.search(context);

        assertEquals(1, result.summary().totalHits());
        assertEquals(SearchMatchType.PATH, result.hits().get(0).matchType());
    }

    @Test
    void testSymbolSearch() {
        SearchQuery query = new SearchQuery("UserController", Collections.emptyMap());
        SearchConfiguration config = new SearchConfiguration();
        config.setSearchPaths(false);
        config.setSearchDependencies(false);

        SearchContext context = new SearchContext(graph, query, config);
        SearchResult result = searchEngine.search(context);

        assertEquals(1, result.summary().totalHits());
        assertEquals(1, result.summary().symbolMatches());
        assertEquals(SearchMatchType.SYMBOL, result.hits().get(0).matchType());
    }

    @Test
    void testDependencySearch() {
        SearchQuery query = new SearchQuery("spring-core", Collections.emptyMap());
        SearchConfiguration config = new SearchConfiguration();
        config.setSearchPaths(false);
        config.setSearchSymbols(false);

        SearchContext context = new SearchContext(graph, query, config);
        SearchResult result = searchEngine.search(context);

        assertEquals(1, result.summary().totalHits());
        assertEquals(1, result.summary().dependencyMatches());
        assertEquals(SearchMatchType.DEPENDENCY, result.hits().get(0).matchType());
    }

    @Test
    void testConfigurationDefaults() {
        SearchConfiguration config = new SearchConfiguration();
        assertFalse(config.caseSensitive());
        assertTrue(config.searchPaths());
        assertTrue(config.searchSymbols());
        assertTrue(config.searchDependencies());
        assertEquals(100, config.maximumResults());
        assertTrue(config.includeMetadata());
    }

    @Test
    void testStatisticsCollection() {
        SearchQuery query = new SearchQuery("UserController", Collections.emptyMap());
        SearchConfiguration config = new SearchConfiguration();
        SearchContext context = new SearchContext(graph, query, config);

        SearchResult result = searchEngine.search(context);

        assertTrue(result.statistics().entitiesScanned() >= 3);
        assertEquals(2, result.statistics().entitiesMatched());
        assertTrue(result.statistics().searchDuration() >= 0);
    }

    @Test
    void testValidatorFailures() {
        SearchValidator validator = new SearchValidator();

        SearchHit hit1 = new SearchHit("file:1", "FILE", "UserController", "src/UserController.java", SearchMatchType.EXACT, Collections.emptyMap());
        SearchHit hit2 = new SearchHit("file:1", "FILE", "UserController", "src/UserController.java", SearchMatchType.EXACT, Collections.emptyMap());

        // 1. Duplicate search hit checking
        List<SearchHit> duplicateHits = List.of(hit1, hit2);
        SearchSummary summary = new SearchSummary(2, 2, 0, 0, 0, 0);
        SearchValidationResult result = validator.validate(duplicateHits, summary, new SearchConfiguration());
        assertFalse(result.isValid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("Duplicate search hit entityId")));

        // 2. Summary count mismatch checking
        SearchSummary inconsistentSummary = new SearchSummary(5, 1, 0, 0, 0, 0);
        SearchValidationResult result3 = validator.validate(List.of(hit1), inconsistentSummary, new SearchConfiguration());
        assertFalse(result3.isValid());
    }

    @Test
    void testIncrementalIndexReuse() {
        SearchQuery query = new SearchQuery("UserController", Collections.emptyMap());
        SearchConfiguration config = new SearchConfiguration();

        // 1. First search run (non-incremental or initial)
        SearchContext context1 = new SearchContext(graph, query, config, "hash-1", false);
        SearchResult result1 = searchEngine.search(context1);
        assertNotNull(result1);

        // 2. Second search run (incremental with matching hash) -> should reuse cache
        SearchContext context2 = new SearchContext(graph, query, config, "hash-1", true);
        SearchResult result2 = searchEngine.search(context2);
        assertEquals(result1.statistics().searchDuration(), result2.statistics().searchDuration()); // confirms cache hit
    }
}
