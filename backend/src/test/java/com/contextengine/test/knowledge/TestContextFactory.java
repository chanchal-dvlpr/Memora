package com.contextengine.test.knowledge;

import com.contextengine.application.knowledge.engine.KnowledgeEngineConfiguration;
import com.contextengine.application.knowledge.engine.KnowledgeEngineContext;
import com.contextengine.application.knowledge.search.SearchConfiguration;
import com.contextengine.application.knowledge.search.SearchContext;
import com.contextengine.application.knowledge.search.SearchQuery;
import com.contextengine.application.scanner.ScanCandidate;
import com.contextengine.application.scanner.SourceSymbol;
import com.contextengine.application.scanner.dependency.ProjectDependency;
import com.contextengine.application.knowledge.graph.KnowledgeGraph;

import java.time.Instant;
import java.util.*;

/**
 * Reusable test factory generating target KnowledgeEngineContext and SearchContext.
 */
public class TestContextFactory {

    public static KnowledgeEngineContext createEngineContext(
        String projectId,
        Collection<ScanCandidate> candidates,
        Collection<SourceSymbol> symbols,
        Collection<ProjectDependency> dependencies
    ) {
        return createEngineContext(projectId, candidates, symbols, dependencies, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false);
    }

    public static KnowledgeEngineContext createEngineContext(
        String projectId,
        Collection<ScanCandidate> candidates,
        Collection<SourceSymbol> symbols,
        Collection<ProjectDependency> dependencies,
        Collection<String> addedPaths,
        Collection<String> modifiedPaths,
        Collection<String> deletedPaths,
        boolean isIncremental
    ) {
        return new KnowledgeEngineContext(
            projectId,
            "workspace-1",
            UUID.randomUUID().toString(),
            "struct-hash-" + UUID.randomUUID(),
            Instant.now(),
            Map.of("processedFiles", candidates.size()),
            new KnowledgeEngineConfiguration(),
            candidates,
            symbols,
            dependencies,
            addedPaths,
            modifiedPaths,
            deletedPaths,
            isIncremental
        );
    }

    public static SearchContext createSearchContext(
        KnowledgeGraph graph,
        String term
    ) {
        return new SearchContext(
            graph,
            new SearchQuery(term, Collections.emptyMap()),
            new SearchConfiguration()
        );
    }
}
