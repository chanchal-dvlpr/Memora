package com.contextengine.application.knowledge.context;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Service orchestrating the retrieval, transformation, selection, validation, and caching of ContextFragments.
 */
public class ContextAssemblyEngineImpl implements ContextAssemblyEngine {

    private final ContextFragmentBuilder fragmentBuilder;
    private final ContextSelectionEngine selectionEngine;
    private final ContextValidator validator;
    private final IncrementalContextAssembler incrementalAssembler;

    public ContextAssemblyEngineImpl() {
        this.fragmentBuilder = new ContextFragmentBuilder();
        this.selectionEngine = new ContextSelectionEngine();
        this.validator = new ContextValidator();
        this.incrementalAssembler = new IncrementalContextAssembler();
    }

    @Override
    public ContextAssemblyResult assemble(ContextAssemblyContext context) {
        Objects.requireNonNull(context, "Context must not be null");
        long start = System.currentTimeMillis();
        ContextStatistics stats = new ContextStatistics();

        // 1. Check for complete reuse if structural hash matches
        if (incrementalAssembler.isUnchanged(context.graph().projectId(), context.structuralHash())) {
            ContextAssemblyResult cachedResult = incrementalAssembler.getCachedResult(context.graph().projectId());
            if (cachedResult != null) {
                stats.setProcessingDuration(System.currentTimeMillis() - start);
                stats.incrementTotalFragments(cachedResult.fragments().size());
                stats.incrementSelectedFragments(cachedResult.fragments().size());
                // Return cached result with updated time
                return new ContextAssemblyResult(
                    cachedResult.projectId(),
                    cachedResult.fragments(),
                    stats,
                    Instant.now()
                );
            }
        }

        // 2. Identify reused fragments in case of incremental run
        Map<String, ContextFragment> reusedFragments = new HashMap<>();
        if (context.isIncremental()) {
            ContextAssemblyResult cachedResult = incrementalAssembler.getCachedResult(context.graph().projectId());
            if (cachedResult != null) {
                Collection<String> dirtyPaths = new ArrayList<>();
                dirtyPaths.addAll(context.addedPaths());
                dirtyPaths.addAll(context.modifiedPaths());
                dirtyPaths.addAll(context.deletedPaths());

                List<ContextFragment> pruned = incrementalAssembler.prune(cachedResult, dirtyPaths);
                for (ContextFragment f : pruned) {
                    reusedFragments.put(f.sourceNodeId(), f);
                }
            }
        }

        // 3. Build Fragments using ContextFragmentBuilder (reusing unaffected ones)
        long assemblyStart = System.currentTimeMillis();
        List<ContextFragment> allFragments = fragmentBuilder.buildFragments(
            context.graph(),
            context.configuration(),
            stats,
            reusedFragments
        );
        long assemblyDuration = System.currentTimeMillis() - assemblyStart;

        // 4. Select Fragments using ContextSelectionEngine
        List<ContextFragment> selectedFragments = selectionEngine.select(
            allFragments,
            context.configuration(),
            stats
        );

        // 5. Validate Fragments using ContextValidator
        long validationStart = System.currentTimeMillis();
        ContextValidationResult validationResult = validator.validate(selectedFragments, context.graph());
        long validationDuration = System.currentTimeMillis() - validationStart;

        // Populate statistics details
        stats.incrementValidationWarnings(validationResult.errors().size());
        stats.setAssemblyDuration(assemblyDuration);
        stats.setValidationDuration(validationDuration);
        stats.setProcessingDuration(System.currentTimeMillis() - start);

        ContextAssemblyResult result = new ContextAssemblyResult(
            context.graph().projectId(),
            selectedFragments,
            stats,
            Instant.now()
        );

        // Cache the newly computed result if valid
        if (validationResult.isValid()) {
            incrementalAssembler.cache(context.graph().projectId(), result, context.structuralHash());
        }

        return result;
    }
}
