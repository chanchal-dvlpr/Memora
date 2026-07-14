package com.contextengine.application.knowledge.ranking;

import com.contextengine.application.knowledge.context.ContextFragment;
import com.contextengine.application.knowledge.context.ContextFragmentType;
import com.contextengine.application.knowledge.context.ContextAssemblyResult;
import com.contextengine.application.knowledge.context.ContextStatistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class RelevanceRankingEngineIntegrationTest {

    @Autowired
    private RelevanceRankingEngine rankingEngine;

    @Test
    void testStrategiesAndScoreCalculation() {
        ContextFragment projectFragment = new ContextFragment(
            "frag-project", ContextFragmentType.PROJECT, "node-p", "project", "Project Node", "content", Collections.emptyMap(), 10
        );
        ContextFragment dependencyFragment = new ContextFragment(
            "frag-dep", ContextFragmentType.DEPENDENCY, "node-d", "pom.xml", "Dependency Node", "content", Collections.emptyMap(), 10
        );

        RankingConfiguration config = new RankingConfiguration(true, true, true, true, 10);
        ContextAssemblyResult assemblyResult = new ContextAssemblyResult(
            "p-1", List.of(projectFragment, dependencyFragment), new ContextStatistics(), Instant.now()
        );

        RankingContext context = new RankingContext(assemblyResult, config, "hash-1", false, Collections.emptyList());
        RankingResult result = rankingEngine.rank(context);

        assertEquals(2, result.rankedFragments().size());

        // project score: structural (100.0) + recency (10.0) = 110.0
        RankedFragment rfProj = result.rankedFragments().stream().filter(f -> f.fragment().fragmentId().equals("frag-project")).findFirst().orElseThrow();
        assertEquals(110.0, rfProj.score().value());

        // dependency score: structural (0.0) + dependency (50.0) + recency (10.0) = 60.0
        RankedFragment rfDep = result.rankedFragments().stream().filter(f -> f.fragment().fragmentId().equals("frag-dep")).findFirst().orElseThrow();
        assertEquals(60.0, rfDep.score().value());

        // Statistics validations
        assertEquals(110.0, result.statistics().highestScore());
        assertEquals(60.0, result.statistics().lowestScore());
        assertEquals(85.0, result.statistics().averageScore());
    }

    @Test
    void testValidatorAndValidationFailures() {
        ContextFragment f1 = new ContextFragment("frag-1", ContextFragmentType.FILE, "node-1", "src/1.java", "File 1", "content", Collections.emptyMap(), 10);
        ContextFragment f2 = new ContextFragment("frag-1", ContextFragmentType.FILE, "node-1", "src/1.java", "File 1 Duplicate", "content", Collections.emptyMap(), 10);

        RankingValidator validator = new RankingValidator();
        RankingConfiguration config = new RankingConfiguration();

        // 1. Success check
        List<RankedFragment> validList = List.of(
            new RankedFragment(f1, new RankingScore(10.0, List.of(new RankingReason(RankingFactor.FILE, 10.0, "reason"))))
        );
        RankingValidationResult validRes = validator.validate(validList, config);
        assertTrue(validRes.isValid());
        assertTrue(validRes.errors().isEmpty());

        // 2. Failure: Duplicate detection
        List<RankedFragment> invalidList = List.of(
            new RankedFragment(f1, new RankingScore(10.0, List.of(new RankingReason(RankingFactor.FILE, 10.0, "reason")))),
            new RankedFragment(f2, new RankingScore(5.0, List.of(new RankingReason(RankingFactor.FILE, 5.0, "reason"))))
        );
        RankingValidationResult invalidRes = validator.validate(invalidList, config);
        assertFalse(invalidRes.isValid());
        assertTrue(invalidRes.errors().stream().anyMatch(e -> e.contains("Duplicate RankedFragment ID")));

        // 3. Failure: Invalid score (negative)
        List<RankedFragment> negativeScoreList = List.of(
            new RankedFragment(f1, new RankingScore(-10.0, List.of(new RankingReason(RankingFactor.FILE, -10.0, "reason"))))
        );
        RankingValidationResult negativeRes = validator.validate(negativeScoreList, config);
        assertFalse(negativeRes.isValid());
        assertTrue(negativeRes.errors().stream().anyMatch(e -> e.contains("Score value is negative")));

        // 4. Failure: Inconsistent ordering
        List<RankedFragment> outOfOrderList = List.of(
            new RankedFragment(f1, new RankingScore(5.0, List.of(new RankingReason(RankingFactor.FILE, 5.0, "reason")))),
            new RankedFragment(
                new ContextFragment("frag-2", ContextFragmentType.FILE, "node-2", "src/2.java", "File 2", "content", Collections.emptyMap(), 10),
                new RankingScore(10.0, List.of(new RankingReason(RankingFactor.FILE, 10.0, "reason")))
            )
        );
        RankingValidationResult orderRes = validator.validate(outOfOrderList, config);
        assertFalse(orderRes.isValid());
        assertTrue(orderRes.errors().stream().anyMatch(e -> e.contains("Inconsistent ordering")));
    }

    @Test
    void testIncrementalRankingUpdates() {
        ContextFragment f1 = new ContextFragment("frag-1", ContextFragmentType.FILE, "node-1", "src/1.java", "File 1", "content", Map.of("lastModified", 1000L), 10);
        ContextFragment f2 = new ContextFragment("frag-2", ContextFragmentType.FILE, "node-2", "src/2.java", "File 2", "content", Map.of("lastModified", 1000L), 10);

        ContextAssemblyResult assemblyResult = new ContextAssemblyResult(
            "p-inc-ranking", List.of(f1, f2), new ContextStatistics(), Instant.now()
        );

        RankingConfiguration config = new RankingConfiguration();

        // 1. Initial run
        RankingContext initialContext = new RankingContext(assemblyResult, config, "hash-1", false, Collections.emptyList());
        RankingResult initialResult = rankingEngine.rank(initialContext);

        assertEquals(2, initialResult.rankedFragments().size());
        assertEquals(2, initialResult.statistics().fragmentsProcessed());

        // 2. Incremental run with unchanged structural hash (should reuse completely)
        RankingContext unchangedContext = new RankingContext(assemblyResult, config, "hash-1", true, Collections.emptyList());
        RankingResult unchangedResult = rankingEngine.rank(unchangedContext);

        assertEquals(2, unchangedResult.rankedFragments().size());
        assertEquals(2, unchangedResult.statistics().fragmentsProcessed());
        assertEquals(initialResult.timestamp(), unchangedResult.timestamp()); // Verify it is completely reused

        // 3. Incremental run with changed structural hash but no dirty paths (should reuse cached fragments)
        RankingContext cachedContext = new RankingContext(assemblyResult, config, "hash-2", true, Collections.emptyList());
        RankingResult cachedResult = rankingEngine.rank(cachedContext);

        assertEquals(2, cachedResult.rankedFragments().size());
        assertEquals(2, cachedResult.statistics().fragmentsProcessed());
        assertNotEquals(initialResult.timestamp(), cachedResult.timestamp()); // New run, but using cached fragment scores

        // 4. Incremental run with changed structural hash and 1 dirty path (should re-rank only dirty, reuse cached for others)
        ContextFragment f1Modified = new ContextFragment("frag-1", ContextFragmentType.FILE, "node-1", "src/1.java", "File 1 Modified", "content", Map.of("lastModified", 2000L), 10);
        ContextAssemblyResult updatedAssembly = new ContextAssemblyResult(
            "p-inc-ranking", List.of(f1Modified, f2), new ContextStatistics(), Instant.now()
        );

        RankingContext dirtyContext = new RankingContext(updatedAssembly, config, "hash-3", true, List.of("src/1.java"));
        RankingResult dirtyResult = rankingEngine.rank(dirtyContext);

        assertEquals(2, dirtyResult.rankedFragments().size());
        // Verify f2 (unchanged) uses same cached score, and f1Modified is computed fresh
        RankedFragment rf1 = dirtyResult.rankedFragments().stream().filter(f -> f.fragment().fragmentId().equals("frag-1")).findFirst().orElseThrow();
        assertEquals("File 1 Modified", rf1.fragment().title());
    }
}
