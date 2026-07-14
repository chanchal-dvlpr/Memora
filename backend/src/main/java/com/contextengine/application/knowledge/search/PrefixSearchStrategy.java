package com.contextengine.application.knowledge.search;

import com.contextengine.application.knowledge.graph.GraphNode;
import java.util.List;

/**
 * Strategy matching file or directory labels by prefix.
 */
public class PrefixSearchStrategy implements SearchAlgorithm {

    @Override
    public boolean match(GraphNode node, String term, SearchConfiguration config, List<SearchHit> hits) {
        if (node.type() != GraphNode.Type.FILE && node.type() != GraphNode.Type.DIRECTORY) {
            return false;
        }
        if (!config.searchPaths()) {
            return false;
        }

        String src = config.caseSensitive() ? node.label() : node.label().toLowerCase();
        String q = config.caseSensitive() ? term : term.toLowerCase();

        if (src.startsWith(q) && !src.equals(q)) {
            hits.add(createHit(node, SearchMatchType.PREFIX, config));
            return true;
        }
        return false;
    }
}
