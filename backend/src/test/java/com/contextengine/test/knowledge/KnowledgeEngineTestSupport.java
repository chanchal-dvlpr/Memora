package com.contextengine.test.knowledge;

import com.contextengine.application.knowledge.engine.KnowledgeEngineContext;
import com.contextengine.application.scanner.ScanCandidate;
import com.contextengine.application.scanner.SourceSymbol;
import com.contextengine.application.scanner.dependency.ProjectDependency;
import java.util.*;

/**
 * Common test support helpers for knowledge engine pipelines.
 */
public class KnowledgeEngineTestSupport {

    public static KnowledgeEngineContext buildMockProjectContext(String projectId, int filesCount, int symbolsCount, int depsCount) {
        Collection<ScanCandidate> candidates = TestProjectFactory.createLargeWorkspace(filesCount);
        List<SourceSymbol> symbols = new ArrayList<>();
        int i = 0;
        for (ScanCandidate candidate : candidates) {
            if (i >= symbolsCount) break;
            symbols.addAll(TestProjectFactory.createSymbolsForFile(candidate.relativePath(), 1));
            i++;
        }
        Collection<ProjectDependency> deps = TestProjectFactory.createDependencyHeavyProject(depsCount);

        return TestContextFactory.createEngineContext(projectId, candidates, symbols, deps);
    }
}
