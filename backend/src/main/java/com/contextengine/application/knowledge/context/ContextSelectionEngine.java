package com.contextengine.application.knowledge.context;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Filters and selects eligible ContextFragments from an assembled list based on rules and configuration.
 */
public class ContextSelectionEngine {

    /**
     * Filters the assembled fragments list.
     *
     * @param fragments assembled fragments
     * @param config    configuration rules
     * @param stats     statistics to update
     * @return filtered ordered list of context fragments
     */
    public List<ContextFragment> select(
        List<ContextFragment> fragments,
        ContextAssemblyConfiguration config,
        ContextStatistics stats
    ) {
        Objects.requireNonNull(fragments, "Fragments list must not be null");
        Objects.requireNonNull(config, "Config must not be null");
        Objects.requireNonNull(stats, "Stats must not be null");

        List<ContextFragment> selected = new ArrayList<>();
        Set<String> processedFragmentIds = new HashSet<>();

        for (ContextFragment fragment : fragments) {
            // Rule 1: include/exclude fragment types
            if (!shouldInclude(fragment.fragmentType(), config)) {
                stats.incrementFilteredFragments(1);
                continue;
            }

            // Rule 2: duplicate elimination
            if (!processedFragmentIds.add(fragment.fragmentId())) {
                stats.incrementDuplicateFragmentsRemoved(1);
                continue;
            }

            // Rule 3: hidden/generated file exclusion
            if (isHiddenOrGenerated(fragment)) {
                stats.incrementFilteredFragments(1);
                continue;
            }

            selected.add(fragment);
            stats.incrementSelectedFragments(1);
        }

        return selected;
    }

    private boolean shouldInclude(ContextFragmentType type, ContextAssemblyConfiguration config) {
        return switch (type) {
            case PROJECT, WORKSPACE -> config.includeWorkspaceMetadata();
            case MODULE -> true;
            case DIRECTORY -> config.includeDirectories();
            case FILE -> true;
            case SYMBOL -> config.includeSymbols();
            case DEPENDENCY -> config.includeDependencies();
        };
    }

    private boolean isHiddenOrGenerated(ContextFragment fragment) {
        // 1. Check properties
        Map<String, Object> metadata = fragment.metadata();
        if (metadata != null) {
            if (Boolean.TRUE.equals(metadata.get("hidden")) || Boolean.TRUE.equals(metadata.get("generated"))) {
                return true;
            }
        }

        // 2. Check path-based conventions
        String path = fragment.sourcePath();
        if (path != null && !path.isEmpty()) {
            if (path.startsWith(".") || path.contains("/.") ||
                path.contains("node_modules") || path.contains("target/") ||
                path.contains("build/") || path.toLowerCase().contains("generated")) {
                return true;
            }
        }
        return false;
    }
}
