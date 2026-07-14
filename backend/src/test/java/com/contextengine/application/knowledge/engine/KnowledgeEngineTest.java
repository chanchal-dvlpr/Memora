package com.contextengine.application.knowledge.engine;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class KnowledgeEngineTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private KnowledgeEngine knowledgeEngine;

    @Test
    void testSpringBeanRegistration() {
        assertNotNull(applicationContext, "Spring ApplicationContext must not be null");
        assertNotNull(knowledgeEngine, "KnowledgeEngine bean must be registered and injected");
        
        KnowledgeEngine retrieved = applicationContext.getBean(KnowledgeEngine.class);
        assertSame(knowledgeEngine, retrieved);
    }

    @Test
    void testConfigurationDefaults() {
        KnowledgeEngineConfiguration config = new KnowledgeEngineConfiguration();
        assertEquals(5, config.maxGraphDepth());
        assertTrue(config.enableDependencyExpansion());
        assertTrue(config.enableSymbolRelationships());
        assertTrue(config.enableSemanticEnrichment());
        assertEquals("STRICT", config.validationMode());
    }

    @Test
    void testContextAndStatistics() {
        KnowledgeEngineConfiguration config = new KnowledgeEngineConfiguration(10, false, false, false, "LAX");
        Map<String, Object> statsMap = new HashMap<>();
        statsMap.put("filesCount", 100);
        statsMap.put("symbolsCount", 500);

        KnowledgeEngineContext context = new KnowledgeEngineContext(
            "test-project-id",
            "test-workspace-id",
            "test-scan-id",
            "test-structural-hash",
            Instant.now(),
            statsMap,
            config
        );

        assertEquals("test-project-id", context.projectId());
        assertEquals("test-workspace-id", context.workspaceId());
        assertEquals("test-scan-id", context.scanId());
        assertEquals("test-structural-hash", context.structuralHash());
        assertEquals(100, context.scannerStatistics().get("filesCount"));
        assertEquals(500, context.scannerStatistics().get("symbolsCount"));
        assertEquals(10, context.configuration().maxGraphDepth());
        assertFalse(context.configuration().enableDependencyExpansion());
    }

    @Test
    void testEngineProcessingCompleted() {
        Map<String, Object> statsMap = new HashMap<>();
        statsMap.put("filesCount", 15);
        statsMap.put("symbolsCount", 45);

        KnowledgeEngineContext context = new KnowledgeEngineContext(
            "p1",
            "w1",
            "s1",
            "hash123",
            Instant.now(),
            statsMap,
            new KnowledgeEngineConfiguration()
        );

        KnowledgeEngineResult result = knowledgeEngine.process(context);

        assertEquals("p1", result.projectId());
        assertEquals("s1", result.scanId());
        assertEquals("COMPLETED", result.processingStatus());
        assertNotNull(result.generatedTimestamp());

        KnowledgeEngineStatistics stats = result.statistics();
        assertEquals(60, stats.nodesProcessed()); // 15 files + 45 symbols
        assertEquals(0, stats.relationshipsProcessed());
        assertEquals(0, stats.warnings());
        assertTrue(stats.processingDurationMs() >= 0);
    }

    @Test
    void testEngineProcessingFailedOnMissingHashInStrict() {
        Map<String, Object> statsMap = new HashMap<>();
        KnowledgeEngineContext context = new KnowledgeEngineContext(
            "p1",
            "w1",
            "s1",
            "", // empty structural hash
            Instant.now(),
            statsMap,
            new KnowledgeEngineConfiguration(5, true, true, true, "STRICT")
        );

        KnowledgeEngineResult result = knowledgeEngine.process(context);
        assertEquals("FAILED", result.processingStatus());
        assertEquals(1, result.statistics().warnings());
    }

    @Test
    void testEngineProcessingWarningOnMissingHashInLax() {
        Map<String, Object> statsMap = new HashMap<>();
        KnowledgeEngineContext context = new KnowledgeEngineContext(
            "p1",
            "w1",
            "s1",
            "", // empty structural hash
            Instant.now(),
            statsMap,
            new KnowledgeEngineConfiguration(5, true, true, true, "LAX")
        );

        KnowledgeEngineResult result = knowledgeEngine.process(context);
        assertEquals("COMPLETED", result.processingStatus());
        assertEquals(1, result.statistics().warnings());
    }

    @Test
    void testStatisticsIncrementors() {
        KnowledgeEngineStatistics stats = new KnowledgeEngineStatistics();
        assertEquals(0, stats.nodesProcessed());
        assertEquals(0, stats.relationshipsProcessed());
        assertEquals(0, stats.warnings());
        assertEquals(0, stats.skippedEntities());

        stats.incrementNodesProcessed(5);
        stats.incrementRelationshipsProcessed(10);
        stats.incrementWarnings(2);
        stats.incrementSkippedEntities(1);
        stats.setProcessingDurationMs(150);

        assertEquals(5, stats.nodesProcessed());
        assertEquals(10, stats.relationshipsProcessed());
        assertEquals(2, stats.warnings());
        assertEquals(1, stats.skippedEntities());
        assertEquals(150, stats.processingDurationMs());
    }
}
