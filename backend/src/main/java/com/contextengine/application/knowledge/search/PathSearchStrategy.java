package com.contextengine.application.knowledge.search;

import com.contextengine.application.knowledge.graph.GraphNode;
import java.util.List;

/**
 * Strategy matching file or directory relative paths.
 */
public class PathSearchStrategy implements SearchAlgorithm {

    @Override
    public boolean match(GraphNode node, String term, SearchConfiguration config, List<SearchHit> hits) {
        if (!config.searchPaths()) return false;
        if (node.type() != GraphNode.Type.FILE && node.type() != GraphNode.Type.DIRECTORY) {
            return false;
        }

        String lbl = config.caseSensitive() ? node.label() : node.label().toLowerCase();
        String q = config.caseSensitive() ? term : term.toLowerCase();
        if (lbl.contains(q)) {
            return false;
        }

        String path = node.id().replace("file:", "").replace("dir:", "");
        String src = config.caseSensitive() ? path : path.toLowerCase();

        if (src.contains(q)) {
            hits.add(createHit(node, SearchMatchType.PATH, config));
            return true;
        }
        return false;
    }
}
