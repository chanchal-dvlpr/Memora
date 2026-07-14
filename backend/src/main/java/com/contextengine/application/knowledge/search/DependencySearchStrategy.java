package com.contextengine.application.knowledge.search;

import com.contextengine.application.knowledge.graph.GraphNode;
import java.util.List;

/**
 * Strategy matching dependency nodes by label.
 */
public class DependencySearchStrategy implements SearchAlgorithm {

    @Override
    public boolean match(GraphNode node, String term, SearchConfiguration config, List<SearchHit> hits) {
        if (!config.searchDependencies() || node.type() != GraphNode.Type.DEPENDENCY) {
            return false;
        }

        String src = config.caseSensitive() ? node.label() : node.label().toLowerCase();
        String q = config.caseSensitive() ? term : term.toLowerCase();

        if (src.contains(q)) {
            hits.add(createHit(node, SearchMatchType.DEPENDENCY, config));
            return true;
        }
        return false;
    }
}
