package com.contextengine.application.knowledge.ranking;

import com.contextengine.application.knowledge.context.ContextFragment;
import com.contextengine.application.knowledge.context.ContextFragmentType;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RelevanceRankingEngineUnitTest {

    @Test
    void testStructuralRankingStrategy() {
        StructuralRankingStrategy strategy = new StructuralRankingStrategy();
        ContextFragment proj = new ContextFragment("frag-1", ContextFragmentType.PROJECT, "n-1", "path", "title", "content", Collections.emptyMap(), 10);
        ContextFragment file = new ContextFragment("frag-2", ContextFragmentType.FILE, "n-2", "path", "title", "content", Collections.emptyMap(), 10);

        // Enabled config
        RankingConfiguration enabledConfig = new RankingConfiguration(true, true, true, true, 100);
        RankingReason r1 = strategy.evaluate(proj, enabledConfig);
        assertEquals(100.0, r1.contribution());
        assertEquals(RankingFactor.STRUCTURAL_IMPORTANCE, r1.factor());

        RankingReason r2 = strategy.evaluate(file, enabledConfig);
        assertEquals(70.0, r2.contribution());

        // Disabled config
        RankingConfiguration disabledConfig = new RankingConfiguration(true, false, true, true, 100);
        RankingReason r3 = strategy.evaluate(proj, disabledConfig);
        assertEquals(0.0, r3.contribution());
    }

    @Test
    void testDependencyRankingStrategy() {
        DependencyRankingStrategy strategy = new DependencyRankingStrategy();
        ContextFragment dep = new ContextFragment("frag-1", ContextFragmentType.DEPENDENCY, "n-1", "path", "title", "content", Collections.emptyMap(), 10);
        ContextFragment file = new ContextFragment("frag-2", ContextFragmentType.FILE, "n-2", "path", "title", "content", Collections.emptyMap(), 10);

        RankingConfiguration enabledConfig = new RankingConfiguration(true, true, true, true, 100);
        RankingReason r1 = strategy.evaluate(dep, enabledConfig);
        assertEquals(50.0, r1.contribution());

        // Disabled config
        RankingConfiguration disabledConfig = new RankingConfiguration(true, true, false, true, 100);
        RankingReason r2 = strategy.evaluate(dep, disabledConfig);
        assertEquals(10.0, r2.contribution());

        // Non-dependency type
        RankingReason r3 = strategy.evaluate(file, enabledConfig);
        assertEquals(0.0, r3.contribution());
    }

    @Test
    void testSymbolRankingStrategy() {
        SymbolRankingStrategy strategy = new SymbolRankingStrategy();
        ContextFragment sym = new ContextFragment("frag-1", ContextFragmentType.SYMBOL, "n-1", "path", "title", "content", Collections.emptyMap(), 10);
        ContextFragment file = new ContextFragment("frag-2", ContextFragmentType.FILE, "n-2", "path", "title", "content", Collections.emptyMap(), 10);

        RankingConfiguration enabledConfig = new RankingConfiguration(true, true, true, true, 100);
        RankingReason r1 = strategy.evaluate(sym, enabledConfig);
        assertEquals(60.0, r1.contribution());

        // Disabled config
        RankingConfiguration disabledConfig = new RankingConfiguration(true, true, true, false, 100);
        RankingReason r2 = strategy.evaluate(sym, disabledConfig);
        assertEquals(15.0, r2.contribution());

        // Non-symbol type
        RankingReason r3 = strategy.evaluate(file, enabledConfig);
        assertEquals(0.0, r3.contribution());
    }

    @Test
    void testRecencyRankingStrategy() {
        RecencyRankingStrategy strategy = new RecencyRankingStrategy();
        ContextFragment f1 = new ContextFragment("frag-1", ContextFragmentType.FILE, "n-1", "path", "title", "content", Collections.emptyMap(), 10);
        ContextFragment f2 = new ContextFragment("frag-2", ContextFragmentType.FILE, "n-2", "path", "title", "content", Map.of("lastModified", 1000L), 10);

        RankingConfiguration enabledConfig = new RankingConfiguration(true, true, true, true, 100);
        RankingReason r1 = strategy.evaluate(f1, enabledConfig);
        assertEquals(10.0, r1.contribution());

        RankingReason r2 = strategy.evaluate(f2, enabledConfig);
        assertEquals(15.0, r2.contribution());

        // Disabled config
        RankingConfiguration disabledConfig = new RankingConfiguration(false, true, true, true, 100);
        RankingReason r3 = strategy.evaluate(f1, disabledConfig);
        assertEquals(0.0, r3.contribution());
    }

    @Test
    void testRankingValidatorEdgeCases() {
        RankingValidator validator = new RankingValidator();
        RankingConfiguration validConfig = new RankingConfiguration();

        ContextFragment f1 = new ContextFragment("frag-1", ContextFragmentType.FILE, "n-1", "path", "title", "content", Collections.emptyMap(), 10);

        // 1. Invalid maximumCandidates limit
        RankingConfiguration badConfig = new RankingConfiguration(true, true, true, true, 0);
        List<RankedFragment> emptyList = Collections.emptyList();
        RankingValidationResult r1 = validator.validate(emptyList, badConfig);
        assertFalse(r1.isValid());
        assertTrue(r1.errors().stream().anyMatch(e -> e.contains("maximumCandidates must be greater than 0")));

        // 2. Null RankedFragment at index
        List<RankedFragment> nullRankedList = java.util.Arrays.asList((RankedFragment) null);
        RankingValidationResult r2 = validator.validate(nullRankedList, validConfig);
        assertFalse(r2.isValid());
        assertTrue(r2.errors().stream().anyMatch(e -> e.contains("RankedFragment at index 0 is null")));

        // 4. NaN / Infinity scores
        RankingScore nanScore = new RankingScore(Double.NaN, List.of(new RankingReason(RankingFactor.FILE, 10.0, "reason")));
        List<RankedFragment> nanList = List.of(new RankedFragment(f1, nanScore));
        RankingValidationResult r4 = validator.validate(nanList, validConfig);
        assertFalse(r4.isValid());
        assertTrue(r4.errors().stream().anyMatch(e -> e.contains("Invalid score value (NaN/Infinite)")));

        // 5. Empty reasons list
        RankingScore noReasonsScore = new RankingScore(10.0, Collections.emptyList());
        List<RankedFragment> noReasonsList = List.of(new RankedFragment(f1, noReasonsScore));
        RankingValidationResult r5 = validator.validate(noReasonsList, validConfig);
        assertFalse(r5.isValid());
        assertTrue(r5.errors().stream().anyMatch(e -> e.contains("No ranking reasons provided")));

        // 6. Negative contribution factor
        RankingScore negativeReasonScore = new RankingScore(10.0, List.of(new RankingReason(RankingFactor.FILE, -5.0, "reason")));
        List<RankedFragment> negativeReasonList = List.of(new RankedFragment(f1, negativeReasonScore));
        RankingValidationResult r6 = validator.validate(negativeReasonList, validConfig);
        assertFalse(r6.isValid());
        assertTrue(r6.errors().stream().anyMatch(e -> e.contains("Negative contribution factor")));
    }

    @Test
    void testIncrementalRankingEngineEdgeCases() {
        IncrementalRankingEngine engine = new IncrementalRankingEngine();

        // Null/empty structural hash checks
        assertFalse(engine.isUnchanged("p-1", null));
        assertFalse(engine.isUnchanged("p-1", ""));

        // Missing cache checks
        assertTrue(engine.getCachedFragments("p-1").isEmpty());
        assertNull(engine.getCachedResult("p-1"));

        // Check clear
        ContextFragment f1 = new ContextFragment("frag-1", ContextFragmentType.FILE, "n-1", "path", "title", "content", Collections.emptyMap(), 10);
        List<RankedFragment> list = List.of(
            new RankedFragment(f1, new RankingScore(10.0, List.of(new RankingReason(RankingFactor.FILE, 10.0, "reason"))))
        );
        engine.cacheResult("p-1", "hash-1", list, null);

        assertTrue(engine.isUnchanged("p-1", "hash-1"));
        assertFalse(engine.getCachedFragments("p-1").isEmpty());

        engine.clear();
        assertFalse(engine.isUnchanged("p-1", "hash-1"));
        assertTrue(engine.getCachedFragments("p-1").isEmpty());
    }
}
