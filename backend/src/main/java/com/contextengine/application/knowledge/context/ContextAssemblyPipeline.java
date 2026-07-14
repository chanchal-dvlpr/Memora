package com.contextengine.application.knowledge.context;

import com.contextengine.application.knowledge.graph.KnowledgeGraph;
import java.util.Objects;

/**
 * Pipeline coordinating context assembly from a KnowledgeGraph.
 */
public class ContextAssemblyPipeline {

    private final ContextAssemblyEngine engine;

    public ContextAssemblyPipeline() {
        this.engine = new ContextAssemblyEngineImpl();
    }

    /**
     * Executes the assembly pipeline.
     *
     * @param graph  target knowledge graph
     * @param config configuration parameters
     * @return result carrier of assembled context fragments
     */
    public ContextAssemblyResult execute(KnowledgeGraph graph, ContextAssemblyConfiguration config) {
        Objects.requireNonNull(graph, "Graph must not be null");
        Objects.requireNonNull(config, "Config must not be null");
        return engine.assemble(new ContextAssemblyContext(graph, config));
    }
}
