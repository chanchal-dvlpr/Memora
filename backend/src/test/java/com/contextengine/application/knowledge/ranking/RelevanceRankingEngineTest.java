package com.contextengine.application.knowledge.ranking;

import com.contextengine.application.knowledge.context.ContextFragment;
import com.contextengine.application.knowledge.context.ContextFragmentType;
import com.contextengine.application.knowledge.context.ContextAssemblyResult;
import com.contextengine.application.knowledge.context.ContextStatistics;
import com.contextengine.application.knowledge.engine.KnowledgeEngine;
import com.contextengine.application.knowledge.engine.KnowledgeEngineConfiguration;
import com.contextengine.application.knowledge.engine.KnowledgeEngineContext;
import com.contextengine.application.knowledge.engine.KnowledgeEngineResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class RelevanceRankingEngineTest {

    @Autowired
    private RelevanceRankingEngine rankingEngine;

    @Autowired
    private KnowledgeEngine knowledgeEngine;

    @Test
    void testConfigurationDefaults() {
        RankingConfiguration config = new RankingConfiguration();
        assertTrue(config.enableRecencyWeight());
        assertTrue(config.enableStructuralWeight());
        assertTrue(config.enableDependencyWeight());
        assertTrue(config.enableSymbolWeight());
        assertEquals(1000, config.maximumCandidates());
    }

    @Test
    void testRankedFragmentDeterministicOrdering() {
        ContextFragment f1 = new ContextFragment("frag-a", ContextFragmentType.FILE, "node-1", "src/a.java", "a", "content a", Collections.emptyMap(), 10);
        ContextFragment f2 = new ContextFragment("frag-b", ContextFragmentType.SYMBOL, "node-2", "src/a.java", "b", "content b", Collections.emptyMap(), 5);
        ContextFragment f3 = new ContextFragment("frag-c", ContextFragmentType.DEPENDENCY, "node-3", "pom.xml", "c", "content c", Collections.emptyMap(), 8);

        // Calculate scores manually to verify compareTo logic
        RankingScore s1 = new RankingScore(80.0, Collections.emptyList());
        RankingScore s2 = new RankingScore(95.0, Collections.emptyList());
        RankingScore s3 = new RankingScore(80.0, Collections.emptyList()); // Same score as s1, should compare lexicographically on ID

        RankedFragment rf1 = new RankedFragment(f1, s1);
        RankedFragment rf2 = new RankedFragment(f2, s2);
        RankedFragment rf3 = new RankedFragment(f3, s3);

        List<RankedFragment> list = new java.util.ArrayList<>(List.of(rf1, rf2, rf3));
        Collections.sort(list);

        // Sorting: descending by score, then lexicographically by ID.
        // 95.0 first (rf2), then 80.0 (rf1 "frag-a" vs rf3 "frag-c" -> "frag-a" comes first)
        assertEquals("frag-b", list.get(0).fragment().fragmentId());
        assertEquals("frag-a", list.get(1).fragment().fragmentId());
        assertEquals("frag-c", list.get(2).fragment().fragmentId());
    }

    @Test
    void testRankingEngineTruncationAndStats() {
        ContextAssemblyResult assemblyResult = new ContextAssemblyResult(
            "p-1",
            List.of(
                new ContextFragment("frag-a", ContextFragmentType.PROJECT, "node-1", "project", "Project", "content", Collections.emptyMap(), 10),
                new ContextFragment("frag-b", ContextFragmentType.FILE, "node-2", "src/b.java", "File B", "content", Collections.emptyMap(), 10),
                new ContextFragment("frag-c", ContextFragmentType.SYMBOL, "node-3", "src/b.java", "Sym C", "content", Collections.emptyMap(), 10)
            ),
            new ContextStatistics(),
            Instant.now()
        );

        RankingConfiguration config = new RankingConfiguration(true, true, true, true, 2);
        RankingContext context = new RankingContext(assemblyResult, config);

        RankingResult result = rankingEngine.rank(context);

        assertEquals(2, result.rankedFragments().size());
        assertEquals(3, result.statistics().fragmentsProcessed());
        assertEquals(3, result.statistics().fragmentsRanked());
        assertEquals(1, result.statistics().skippedFragments());
        assertEquals(0, result.statistics().warnings());
    }

    @Test
    void testRankingPipelineWrapper() {
        ContextAssemblyResult assemblyResult = new ContextAssemblyResult(
            "p-1",
            List.of(
                new ContextFragment("frag-a", ContextFragmentType.PROJECT, "node-1", "project", "Project", "content", Collections.emptyMap(), 10)
            ),
            new ContextStatistics(),
            Instant.now()
        );

        RankingPipeline pipeline = new RankingPipeline();
        RankingResult result = pipeline.execute(assemblyResult, new RankingConfiguration());

        assertNotNull(result);
        assertEquals(1, result.rankedFragments().size());
    }

    @Test
    void testKnowledgeEngineIntegration() {
        // Enable includeWorkspaceMetadata by setting the 4th parameter (enableSemanticEnrichment) to true
        KnowledgeEngineConfiguration config = new KnowledgeEngineConfiguration(10, false, false, true, "STRICT");
        KnowledgeEngineContext context = new KnowledgeEngineContext(
            "p-test-ranking", "w-test-ranking", "s-1", "hash-ok", Instant.now(), Collections.emptyMap(), config,
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false
        );

        KnowledgeEngineResult result = knowledgeEngine.process(context);

        assertEquals("COMPLETED", result.processingStatus());
        assertNotNull(result.rankingResult());
        // Empty scan yields Project and Workspace fragments, so size should be 2
        assertEquals(2, result.rankingResult().rankedFragments().size());
        assertEquals(2, result.rankingResult().statistics().fragmentsProcessed());
    }
}
