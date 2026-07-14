package com.contextengine.application.knowledge.search;

import com.contextengine.application.knowledge.graph.GraphNode;
import java.util.List;

/**
 * Strategy matching file or directory labels exactly.
 */
public class ExactSearchStrategy implements SearchAlgorithm {

    @Override
    public boolean match(GraphNode node, String term, SearchConfiguration config, List<SearchHit> hits) {
        if (node.type() != GraphNode.Type.FILE && node.type() != GraphNode.Type.DIRECTORY) {
            return false;
        }
        if (!config.searchPaths()) {
            return false;
        }

        boolean isMatch = config.caseSensitive() 
            ? node.label().equals(term) 
            : node.label().equalsIgnoreCase(term);

        if (isMatch) {
            hits.add(createHit(node, SearchMatchType.EXACT, config));
            return true;
        }
        return false;
    }
}
