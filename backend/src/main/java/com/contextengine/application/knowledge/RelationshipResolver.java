package com.contextengine.application.knowledge;

import com.contextengine.domain.entity.KnowledgeGraph;
import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.entity.KnowledgeRelationship;
import com.contextengine.domain.valueobject.GraphWeight;
import com.contextengine.domain.valueobject.NodeId;
import com.contextengine.domain.valueobject.RelationshipId;
import java.util.Objects;
import java.util.UUID;

/**
 * Resolves relationship edges between nodes in the Knowledge Graph.
 * <p>
 * Bounded Context: Knowledge Graph
 * Related Subsystem: Knowledge Graph Engine (KG-ENG)
 * Reference: Functional Requirement FR-016 (Knowledge Graph Engine) Section 8
 * </p>
 */
public class RelationshipResolver {

    /**
     * Constructs a RelationshipResolver.
     */
    public RelationshipResolver() {
    }

    /**
     * Resolves and establishes directed edge relationships in the provided KnowledgeGraph.
     *
     * @param graph the knowledge graph aggregate root
     * @param context the construction session context
     */
    public void resolve(KnowledgeGraph graph, KnowledgeContext context) {
        Objects.requireNonNull(graph, "KnowledgeGraph must not be null");
        Objects.requireNonNull(context, "KnowledgeContext must not be null");

        // 1. Resolve BELONGS_TO relationships between FILE nodes and the PROJECT node
        KnowledgeNode projectNode = graph.nodes().stream()
            .filter(n -> n.type().equals("PROJECT"))
            .findFirst()
            .orElse(null);

        if (projectNode != null) {
            for (KnowledgeNode node : graph.nodes()) {
                if (node.type().equals("FILE")) {
                    addEdgeIfValid(graph, node.id(), projectNode.id(), "BELONGS_TO");
                }
            }
        }

        // 2. Resolve BELONGS_TO relationships between Symbol nodes and their containing FILE node
        for (KnowledgeNode node : graph.nodes()) {
            if (!node.type().equals("PROJECT") && !node.type().equals("FILE")) {
                String filePath = node.attributes().get("filePath");
                if (filePath != null) {
                    // Find containing file node
                    KnowledgeNode fileNode = graph.nodes().stream()
                        .filter(n -> n.type().equals("FILE") && n.attributes().get("name").equals(filePath))
                        .findFirst()
                        .orElse(null);
                    if (fileNode != null) {
                        addEdgeIfValid(graph, node.id(), fileNode.id(), "BELONGS_TO");
                    }
                }
            }
        }

        // 3. Resolve REFERENCES relationships between symbols
        for (KnowledgeNode node : graph.nodes()) {
            String refsAttr = node.attributes().get("references");
            if (refsAttr != null && !refsAttr.trim().isEmpty()) {
                String[] references = refsAttr.split(",");
                for (String ref : references) {
                    String cleanRef = ref.trim();
                    if (!cleanRef.isEmpty()) {
                        // Find matching target symbol node in the graph by name
                        KnowledgeNode targetNode = graph.nodes().stream()
                            .filter(n -> !n.type().equals("PROJECT") && !n.type().equals("FILE") && n.attributes().get("name").equals(cleanRef))
                            .findFirst()
                            .orElse(null);
                        if (targetNode != null && !node.id().equals(targetNode.id())) {
                            addEdgeIfValid(graph, node.id(), targetNode.id(), "REFERENCES");
                        }
                    }
                }
            }
        }
    }

    private void addEdgeIfValid(KnowledgeGraph graph, NodeId sourceId, NodeId targetId, String type) {
        String sourceUrn = graph.nodes().stream().filter(n -> n.id().equals(sourceId)).map(n -> n.attributes().get("urn")).findFirst().orElse("");
        String targetUrn = graph.nodes().stream().filter(n -> n.id().equals(targetId)).map(n -> n.attributes().get("urn")).findFirst().orElse("");

        // Generate SHA-256-derived deterministic UUID representation using the factory
        RelationshipId relId = RelationshipIdFactory.create(sourceUrn, targetUrn, type);

        boolean alreadyExists = graph.relationships().stream()
            .anyMatch(r -> r.id().equals(relId) || 
                          (r.sourceNodeId().equals(sourceId) && r.targetNodeId().equals(targetId) && r.type().equals(type)));

        if (!alreadyExists && !sourceId.equals(targetId)) {
            graph.addRelationship(new KnowledgeRelationship(relId, sourceId, targetId, type, new GraphWeight(1.0)));
        }
    }
}
