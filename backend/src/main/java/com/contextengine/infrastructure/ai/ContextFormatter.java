package com.contextengine.infrastructure.ai;

import com.contextengine.domain.entity.KnowledgeNode;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Transforms raw context data into standard formats optimized for Large Language Model processing.
 * <p>
 * Bounded Context: Context Assembly
 * Related Subsystem: AI Integration Subsystem (AI-SUB)
 * </p>
 */
public class ContextFormatter {

    /**
     * Serializes context elements into token-efficient markdown code blocks.
     *
     * @param nodes the nodes to serialize
     * @return formatted context string
     */
    public String formatAsMarkdown(Collection<KnowledgeNode> nodes) {
        Objects.requireNonNull(nodes, "Nodes must not be null");
        return nodes.stream()
            .map(node -> {
                String name = node.attributes().get("name");
                String path = node.attributes().get("path");
                String kind = node.attributes().get("kind");
                return "### Symbol: " + name + " (" + kind + ")\n" +
                       "* Path: " + path + "\n" +
                       "* Type: " + node.type() + "\n";
            })
            .collect(Collectors.joining("\n"));
    }
}
