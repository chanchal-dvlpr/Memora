package com.contextengine.application.knowledge.budget;

import com.contextengine.application.knowledge.context.ContextFragment;
import com.contextengine.application.knowledge.context.ContextFragmentType;
import com.contextengine.application.knowledge.ranking.RankedFragment;
import com.contextengine.application.knowledge.ranking.RankingResult;
import com.contextengine.application.knowledge.ranking.RankingScore;
import com.contextengine.application.knowledge.ranking.RankingStatistics;
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
class ContextBudgetManagerTest {

    @Autowired
    private ContextBudgetManager budgetManager;

    @Test
    void testConfigurationDefaults() {
        BudgetConfiguration config = new BudgetConfiguration();
        assertEquals(100000, config.maximumTokens());
        assertEquals(400000, config.maximumCharacters());
        assertEquals(100, config.maximumFragments());
        assertEquals(10000, config.reserveSystemTokens());
        assertEquals(20000, config.reserveResponseTokens());
    }

    @Test
    void testDeterministicBudgetingAndStats() {
        ContextFragment proj = new ContextFragment("f-proj", ContextFragmentType.PROJECT, "n-p", "project", "Proj", "proj-content", Collections.emptyMap(), 10);
        ContextFragment file1 = new ContextFragment("f-1", ContextFragmentType.FILE, "n-1", "src/File1.java", "File1", "x".repeat(100), Collections.emptyMap(), 10);
        ContextFragment file2 = new ContextFragment("f-2", ContextFragmentType.FILE, "n-2", "src/File2.java", "File2", "y".repeat(200), Collections.emptyMap(), 10);

        List<RankedFragment> ranked = List.of(
            new RankedFragment(proj, new RankingScore(100.0, Collections.emptyList())),
            new RankedFragment(file1, new RankingScore(80.0, Collections.emptyList())),
            new RankedFragment(file2, new RankingScore(60.0, Collections.emptyList()))
        );

        RankingResult rankingResult = new RankingResult("p-1", ranked, new RankingStatistics(), Instant.now());
        BudgetConfiguration config = new BudgetConfiguration(100000, 400000, 1, 10000, 20000); // maximumFragments = 1

        BudgetContext budgetContext = new BudgetContext(rankingResult, config, "hash-1", false, Collections.emptyList());
        BudgetResult result = budgetManager.budget(budgetContext);

        assertEquals("p-1", result.projectId());
        assertEquals(3, result.budgetedFragments().size());

        // f-proj -> RESERVED
        BudgetedFragment bfProj = result.budgetedFragments().get(0);
        assertEquals(BudgetDecision.RESERVED, bfProj.decision());
        assertEquals(BudgetReason.RESERVED_SYSTEM_CONTEXT, bfProj.reason());

        // f-1 -> INCLUDED
        BudgetedFragment bf1 = result.budgetedFragments().get(1);
        assertEquals(BudgetDecision.INCLUDED, bf1.decision());
        assertEquals(BudgetReason.NONE, bf1.reason());

        // f-2 -> EXCLUDED (maximumFragments = 1 exceeded)
        BudgetedFragment bf2 = result.budgetedFragments().get(2);
        assertEquals(BudgetDecision.EXCLUDED, bf2.decision());
        assertEquals(BudgetReason.MAX_FRAGMENT_LIMIT, bf2.reason());

        // Statistics validations
        BudgetStatistics stats = result.statistics();
        assertEquals(3, stats.fragmentsEvaluated());
        assertEquals(2, stats.fragmentsIncluded()); // project + File1 = 2
        assertEquals(1, stats.fragmentsExcluded());
        assertEquals(112, stats.estimatedCharactersUsed()); // proj-content (12) + File1 (100) = 112
        assertEquals(28, stats.estimatedTokensUsed()); // Math.max(1, 12/4) = 3 + Math.max(1, 100/4) = 25 -> 28
    }

    @Test
    void testTokenAndCharacterLimits() {
        ContextFragment file1 = new ContextFragment("f-1", ContextFragmentType.FILE, "n-1", "src/File1.java", "File1", "x".repeat(100), Collections.emptyMap(), 10);

        List<RankedFragment> ranked = List.of(
            new RankedFragment(file1, new RankingScore(80.0, Collections.emptyList()))
        );

        RankingResult rankingResult = new RankingResult("p-1", ranked, new RankingStatistics(), Instant.now());

        // Character limit check
        BudgetConfiguration charLimitConfig = new BudgetConfiguration(100000, 50, 100, 10000, 20000);
        BudgetResult charResult = budgetManager.budget(new BudgetContext(rankingResult, charLimitConfig));
        assertEquals(BudgetDecision.EXCLUDED, charResult.budgetedFragments().get(0).decision());
        assertEquals(BudgetReason.CHARACTER_LIMIT, charResult.budgetedFragments().get(0).reason());

        // Token limit check (available standard tokens = 30000 - 10000 - 19990 = 10 tokens. Fragment needs 25 tokens)
        BudgetConfiguration tokenLimitConfig = new BudgetConfiguration(30000, 400000, 100, 10000, 19990);
        BudgetResult tokenResult = budgetManager.budget(new BudgetContext(rankingResult, tokenLimitConfig));
        assertEquals(BudgetDecision.EXCLUDED, tokenResult.budgetedFragments().get(0).decision());
        assertEquals(BudgetReason.TOKEN_LIMIT, tokenResult.budgetedFragments().get(0).reason());
    }

    @Test
    void testValidationFailures() {
        ContextFragment f1 = new ContextFragment("f-1", ContextFragmentType.FILE, "n-1", "src/File1.java", "File1", "content", Collections.emptyMap(), 10);
        RankedFragment rf1 = new RankedFragment(f1, new RankingScore(10.0, Collections.emptyList()));

        BudgetValidator validator = new BudgetValidator();
        BudgetConfiguration config = new BudgetConfiguration();

        // 1. Exceeded Token limit validation check
        List<BudgetedFragment> badTokenList = List.of(
            new BudgetedFragment(rf1, BudgetDecision.INCLUDED, BudgetReason.NONE)
        );
        BudgetConfiguration smallTokensConfig = new BudgetConfiguration(0, 400000, 100, 0, 0);
        BudgetValidationResult tokenResult = validator.validate(badTokenList, smallTokensConfig);
        assertFalse(tokenResult.isValid());
        assertTrue(tokenResult.errors().stream().anyMatch(e -> e.contains("Total allocated tokens")));

        // 2. Exceeded Characters limit validation check
        BudgetConfiguration smallCharsConfig = new BudgetConfiguration(100000, 0, 100, 0, 0);
        BudgetValidationResult charResult = validator.validate(badTokenList, smallCharsConfig);
        assertFalse(charResult.isValid());
        assertTrue(charResult.errors().stream().anyMatch(e -> e.contains("Total allocated characters")));

        // 3. Duplicate fragment validation check
        List<BudgetedFragment> duplicateList = List.of(
            new BudgetedFragment(rf1, BudgetDecision.INCLUDED, BudgetReason.NONE),
            new BudgetedFragment(rf1, BudgetDecision.INCLUDED, BudgetReason.NONE)
        );
        BudgetValidationResult duplicateResult = validator.validate(duplicateList, config);
        assertFalse(duplicateResult.isValid());
        assertTrue(duplicateResult.errors().stream().anyMatch(e -> e.contains("Duplicate BudgetedFragment ID")));

        // 4. Invalid Reservation calculation validation check
        List<BudgetedFragment> badReservedList = List.of(
            new BudgetedFragment(rf1, BudgetDecision.RESERVED, BudgetReason.NONE)
        );
        BudgetValidationResult reservedResult = validator.validate(badReservedList, config);
        assertFalse(reservedResult.isValid());
        assertTrue(reservedResult.errors().stream().anyMatch(e -> e.contains("Reserved fragment at index 0 has invalid reason")));
    }

    @Test
    void testIncrementalBudgetingReuse() {
        ContextFragment f1 = new ContextFragment("f-1", ContextFragmentType.FILE, "n-1", "src/File1.java", "File1", "content1", Collections.emptyMap(), 10);
        ContextFragment f2 = new ContextFragment("f-2", ContextFragmentType.FILE, "n-2", "src/File2.java", "File2", "content2", Collections.emptyMap(), 10);

        List<RankedFragment> ranked = List.of(
            new RankedFragment(f1, new RankingScore(80.0, Collections.emptyList())),
            new RankedFragment(f2, new RankingScore(60.0, Collections.emptyList()))
        );

        RankingResult rankingResult = new RankingResult("p-inc-budget", ranked, new RankingStatistics(), Instant.now());
        BudgetConfiguration config = new BudgetConfiguration();

        // 1. Initial run
        BudgetContext initialContext = new BudgetContext(rankingResult, config, "hash-1", false, Collections.emptyList());
        BudgetResult initialResult = budgetManager.budget(initialContext);

        assertEquals(2, initialResult.budgetedFragments().size());

        // 2. Complete reuse with unchanged structural hash
        BudgetContext unchangedContext = new BudgetContext(rankingResult, config, "hash-1", true, Collections.emptyList());
        BudgetResult unchangedResult = budgetManager.budget(unchangedContext);

        assertEquals(initialResult.timestamp(), unchangedResult.timestamp()); // Verified completely reused!

        // 3. Partial reuse where all fragments are cached (structural hash changed, but no dirty paths)
        BudgetContext cachedContext = new BudgetContext(rankingResult, config, "hash-2", true, Collections.emptyList());
        BudgetResult cachedResult = budgetManager.budget(cachedContext);

        assertNotEquals(initialResult.timestamp(), cachedResult.timestamp()); // New run, but using cached fragment decisions
        assertEquals(2, cachedResult.budgetedFragments().size());

        // 4. Partial re-budgeting (structural hash changed, and 1 dirty path)
        ContextFragment f1Modified = new ContextFragment("f-1", ContextFragmentType.FILE, "n-1", "src/File1.java", "File1 Modified", "content1-updated", Collections.emptyMap(), 10);
        List<RankedFragment> updatedRanked = List.of(
            new RankedFragment(f1Modified, new RankingScore(85.0, Collections.emptyList())),
            new RankedFragment(f2, new RankingScore(60.0, Collections.emptyList()))
        );
        RankingResult updatedRanking = new RankingResult("p-inc-budget", updatedRanked, new RankingStatistics(), Instant.now());

        BudgetContext dirtyContext = new BudgetContext(updatedRanking, config, "hash-3", true, List.of("src/File1.java"));
        BudgetResult dirtyResult = budgetManager.budget(dirtyContext);

        assertEquals(2, dirtyResult.budgetedFragments().size());
        BudgetedFragment bf1 = dirtyResult.budgetedFragments().stream().filter(bf -> bf.rankedFragment().fragment().fragmentId().equals("f-1")).findFirst().orElseThrow();
        assertEquals("File1 Modified", bf1.rankedFragment().fragment().title());
    }

    @Test
    void testSpringBeanRegistration() {
        assertNotNull(budgetManager);
    }
}
