package com.contextengine.test.knowledge;

import com.contextengine.application.knowledge.context.*;
import com.contextengine.application.knowledge.ranking.*;
import com.contextengine.application.knowledge.budget.*;
import com.contextengine.application.knowledge.snapshot.*;
import com.contextengine.application.knowledge.search.*;
import com.contextengine.application.knowledge.engine.*;
import com.contextengine.application.scanner.ScanCandidate;
import com.contextengine.application.scanner.SourceSymbol;
import com.contextengine.application.scanner.dependency.ProjectDependency;
import com.contextengine.test.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies integration between Scanner, Graph, Assembly, Ranking, Budgeting, Comparison, and Search.
 */
class KnowledgeEngineIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private KnowledgeEngine knowledgeEngine;

    @Autowired
    private SearchEngine searchEngine;

    @Test
    void testFullIntegrationFlow() {
        String projectId = UUID.randomUUID().toString();

        // 1. Prepare scanner outputs
        Collection<ScanCandidate> candidates = TestProjectFactory.createMultiModuleProject();
        Collection<SourceSymbol> symbols = TestProjectFactory.createSymbolsForFile("module-a/src/ServiceA.java", 3);
        Collection<ProjectDependency> dependencies = TestProjectFactory.createDependencyHeavyProject(2);

        // 2. Invoke KnowledgeEngine
        KnowledgeEngineContext context = TestContextFactory.createEngineContext(
            projectId, candidates, symbols, dependencies
        );

        KnowledgeEngineResult result = knowledgeEngine.process(context);

        assertNotNull(result);
        assertEquals("COMPLETED", result.processingStatus());
        assertNotNull(result.graph());
        assertNotNull(result.contextAssemblyResult());
        assertNotNull(result.rankingResult());
        assertNotNull(result.budgetResult());
        assertNotNull(result.snapshotComparisonResult());
        assertNotNull(result.searchResult());

        // Verify Graph structure
        assertTrue(result.graph().nodes().size() >= 5);

        // Verify Context Assembly Result
        assertTrue(result.contextAssemblyResult().fragments().size() > 0);

        // Verify Relevance Ranking Result
        assertTrue(result.rankingResult().rankedFragments().size() > 0);
        assertTrue(result.rankingResult().rankedFragments().get(0).score().value() >= 0.0);

        // Verify Context Budget Result
        assertTrue(result.budgetResult().budgetedFragments().size() > 0);

        // Verify Search Engine query on the built graph
        SearchQuery query = new SearchQuery("ServiceA", Collections.emptyMap());
        SearchConfiguration config = new SearchConfiguration();
        SearchResult searchResult = searchEngine.search(new SearchContext(result.graph(), query, config));

        assertNotNull(searchResult);
        assertTrue(searchResult.hits().size() > 0);
        assertEquals(SearchMatchType.PREFIX, searchResult.hits().get(0).matchType());
    }

    @Test
    void verifyDeterministicExecution() {
        String projectId = UUID.randomUUID().toString();
        Collection<ScanCandidate> candidates = TestProjectFactory.createSingleFileProject();
        Collection<SourceSymbol> symbols = TestProjectFactory.createSymbolsForFile("src/Main.java", 1);
        Collection<ProjectDependency> dependencies = TestProjectFactory.createDependencyHeavyProject(1);

        KnowledgeEngineContext context1 = TestContextFactory.createEngineContext(projectId, candidates, symbols, dependencies);
        KnowledgeEngineContext context2 = TestContextFactory.createEngineContext(projectId, candidates, symbols, dependencies);

        KnowledgeEngineResult result1 = knowledgeEngine.process(context1);
        KnowledgeEngineResult result2 = knowledgeEngine.process(context2);

        assertEquals(result1.contextAssemblyResult().fragments().size(), result2.contextAssemblyResult().fragments().size());
        assertEquals(result1.rankingResult().rankedFragments().size(), result2.rankingResult().rankedFragments().size());
        for (int i = 0; i < result1.rankingResult().rankedFragments().size(); i++) {
            assertEquals(result1.rankingResult().rankedFragments().get(i).score().value(), 
                         result2.rankingResult().rankedFragments().get(i).score().value());
        }
        assertEquals(result1.budgetResult().budgetedFragments().size(), result2.budgetResult().budgetedFragments().size());
    }
}
