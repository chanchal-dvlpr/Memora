package com.contextengine.test.knowledge;

import com.contextengine.application.knowledge.context.*;
import com.contextengine.application.knowledge.ranking.*;
import com.contextengine.application.knowledge.budget.*;
import com.contextengine.application.knowledge.snapshot.*;
import com.contextengine.application.knowledge.search.*;
import com.contextengine.application.knowledge.engine.*;
import com.contextengine.application.scanner.ScanCandidate;
import com.contextengine.application.scanner.SourceSymbol;
import com.contextengine.application.scanner.SupportedLanguage;
import com.contextengine.application.scanner.dependency.ProjectDependency;
import com.contextengine.test.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End tests verifying workspace updates, incremental updates, renames, moves, and dependency changes.
 */
class KnowledgeEngineEndToEndTest extends BaseIntegrationTest {

    @Autowired
    private KnowledgeEngine knowledgeEngine;

    @Test
    void testEmptyWorkspaceWorkflow() {
        String projectId = UUID.randomUUID().toString();
        KnowledgeEngineContext context = TestContextFactory.createEngineContext(
            projectId,
            TestProjectFactory.createEmptyWorkspace(),
            Collections.emptyList(),
            Collections.emptyList()
        );

        KnowledgeEngineResult result = knowledgeEngine.process(context);
        assertNotNull(result);
        assertEquals("COMPLETED", result.processingStatus());
        assertNotNull(result.contextAssemblyResult().fragments());
    }

    @Test
    void testLargeWorkspaceWorkflow() {
        String projectId = UUID.randomUUID().toString();
        KnowledgeEngineContext context = TestContextFactory.createEngineContext(
            projectId,
            TestProjectFactory.createLargeWorkspace(100),
            Collections.emptyList(),
            Collections.emptyList()
        );

        KnowledgeEngineResult result = knowledgeEngine.process(context);
        assertNotNull(result);
        assertEquals("COMPLETED", result.processingStatus());
        assertTrue(result.graph().nodes().size() >= 100);
    }

    @Test
    void testIncrementalScanWorkflow() {
        String projectId = UUID.randomUUID().toString();

        // 1. Initial Scan
        Collection<ScanCandidate> initialCandidates = List.of(
            new ScanCandidate("src/App.java", "/workspace/src/App.java", 500L, Instant.now(), "FILE", SupportedLanguage.JAVA)
        );
        KnowledgeEngineContext initCtx = TestContextFactory.createEngineContext(
            projectId, initialCandidates, Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false
        );
        KnowledgeEngineResult initResult = knowledgeEngine.process(initCtx);
        assertNotNull(initResult);

        // 2. Incremental update: Add a file
        Collection<ScanCandidate> updatedCandidates = List.of(
            new ScanCandidate("src/App.java", "/workspace/src/App.java", 500L, Instant.now(), "FILE", SupportedLanguage.JAVA),
            new ScanCandidate("src/Helper.java", "/workspace/src/Helper.java", 300L, Instant.now(), "FILE", SupportedLanguage.JAVA)
        );
        KnowledgeEngineContext incCtx = TestContextFactory.createEngineContext(
            projectId, updatedCandidates, Collections.emptyList(), Collections.emptyList(),
            List.of("src/Helper.java"), Collections.emptyList(), Collections.emptyList(), true
        );
        KnowledgeEngineResult incResult = knowledgeEngine.process(incCtx);
        assertNotNull(incResult);
        assertEquals("COMPLETED", incResult.processingStatus());
        assertTrue(incResult.graph().nodes().size() >= 2);
    }

    @Test
    void testRenamedAndMovedFilesWorkflow() {
        String projectId = UUID.randomUUID().toString();

        // 1. Initial State
        Collection<ScanCandidate> step1 = List.of(
            new ScanCandidate("src/Old.java", "/workspace/src/Old.java", 500L, Instant.now(), "FILE", SupportedLanguage.JAVA)
        );
        KnowledgeEngineContext ctx1 = TestContextFactory.createEngineContext(
            projectId, step1, Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false
        );
        knowledgeEngine.process(ctx1);

        // 2. Rename Old.java -> New.java
        Collection<ScanCandidate> step2 = List.of(
            new ScanCandidate("src/New.java", "/workspace/src/New.java", 500L, Instant.now(), "FILE", SupportedLanguage.JAVA)
        );
        KnowledgeEngineContext ctx2 = TestContextFactory.createEngineContext(
            projectId, step2, Collections.emptyList(), Collections.emptyList(),
            List.of("src/New.java"), Collections.emptyList(), List.of("src/Old.java"), true
        );
        KnowledgeEngineResult result2 = knowledgeEngine.process(ctx2);

        assertNotNull(result2);
        assertNotNull(result2.snapshotComparisonResult());
        assertTrue(result2.snapshotComparisonResult().differences().size() > 0);
    }

    @Test
    void testDependencyAndSymbolUpdates() {
        String projectId = UUID.randomUUID().toString();

        // 1. Initial State with spring-core
        Collection<ScanCandidate> candidates = TestProjectFactory.createSingleFileProject();
        Collection<ProjectDependency> dep1 = List.of(new ProjectDependency("spring-core", "1.0.0", "MAVEN", "COMPILE"));
        Collection<SourceSymbol> sym1 = List.of(new SourceSymbol("Main", "CLASS", "src/Main.java", 1, 10, Map.of()));

        KnowledgeEngineContext ctx1 = TestContextFactory.createEngineContext(projectId, candidates, sym1, dep1);
        KnowledgeEngineResult res1 = knowledgeEngine.process(ctx1);

        // 2. Dependency update version to 2.0.0 and symbol update (new Symbol added)
        Collection<ProjectDependency> dep2 = List.of(new ProjectDependency("spring-core", "2.0.0", "MAVEN", "COMPILE"));
        Collection<SourceSymbol> sym2 = List.of(
            new SourceSymbol("Main", "CLASS", "src/Main.java", 1, 10, Map.of()),
            new SourceSymbol("helperMethod", "METHOD", "src/Main.java", 11, 20, Map.of())
        );

        KnowledgeEngineContext ctx2 = TestContextFactory.createEngineContext(projectId, candidates, sym2, dep2);
        KnowledgeEngineResult res2 = knowledgeEngine.process(ctx2);

        assertNotNull(res2);
        assertEquals("COMPLETED", res2.processingStatus());
    }
}
