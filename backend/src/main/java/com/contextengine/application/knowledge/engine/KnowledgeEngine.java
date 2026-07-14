package com.contextengine.application.knowledge.engine;

/**
 * Orchestrator API for knowledge extraction, semantic enrichment, and prompt compilation.
 */
public interface KnowledgeEngine {

    /**
     * Executes knowledge transformations on the validated scanner outputs.
     *
     * @param context the engine processing context
     * @return the transformation results
     */
    KnowledgeEngineResult process(KnowledgeEngineContext context);
}
