package com.contextengine.application.knowledge.context;

/**
 * Coordinate context assembly, producing ordered collections of ContextFragments.
 */
public interface ContextAssemblyEngine {
    ContextAssemblyResult assemble(ContextAssemblyContext context);
}
