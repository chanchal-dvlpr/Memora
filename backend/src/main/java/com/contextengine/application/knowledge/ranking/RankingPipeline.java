package com.contextengine.application.knowledge.ranking;

import com.contextengine.application.knowledge.context.ContextAssemblyResult;
import java.util.Objects;

/**
 * Pipeline coordinating relevance ranking from a ContextAssemblyResult.
 */
public class RankingPipeline {

    private final RelevanceRankingEngine engine;

    public RankingPipeline() {
        this.engine = new RelevanceRankingEngineImpl();
    }

    /**
     * Executes the ranking pipeline.
     *
     * @param assemblyResult context assembly result input
     * @param config         ranking configuration rules
     * @return ranking result payload
     */
    public RankingResult execute(ContextAssemblyResult assemblyResult, RankingConfiguration config) {
        Objects.requireNonNull(assemblyResult, "AssemblyResult must not be null");
        Objects.requireNonNull(config, "Config must not be null");
        return engine.rank(new RankingContext(assemblyResult, config));
    }
}
