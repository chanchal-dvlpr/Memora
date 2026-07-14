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

        // Pre-build index maps to resolve nodes in O(1)
        java.util.Map<String, KnowledgeNode> fileNodesMap = new java.util.HashMap<>();
        java.util.Map<String, KnowledgeNode> symbolNodesMap = new java.util.HashMap<>();
        java.util.Map<NodeId, String> nodeIdToUrnMap = new java.util.HashMap<>();
        
        KnowledgeNode projectNode = null;

        for (KnowledgeNode n : graph.nodes()) {
            nodeIdToUrnMap.put(n.id(), n.attributes().get("urn") != null ? n.attributes().get("urn") : "");
            if (n.type().equals("PROJECT")) {
                projectNode = n;
            } else if (n.type().equals("FILE")) {
                fileNodesMap.put(n.attributes().get("name"), n);
            } else {
                symbolNodesMap.put(n.attributes().get("name"), n);
            }
        }

        // Cache existing relationships for O(1) duplicate checking
        java.util.Set<String> existingRels = new java.util.HashSet<>();
        for (KnowledgeRelationship r : graph.relationships()) {
            existingRels.add(r.id().value().toString());
            existingRels.add(r.sourceNodeId().value().toString() + "->" + r.targetNodeId().value().toString() + ":" + r.type());
        }

        // 1. Resolve BELONGS_TO relationships between FILE nodes and the PROJECT node
        if (projectNode != null) {
            for (KnowledgeNode node : graph.nodes()) {
                if (node.type().equals("FILE")) {
                    addEdgeIfValid(graph, node.id(), projectNode.id(), "BELONGS_TO", nodeIdToUrnMap, existingRels);
                }
            }
        }

        // 2. Resolve BELONGS_TO relationships between Symbol nodes and their containing FILE node
        for (KnowledgeNode node : graph.nodes()) {
            if (!node.type().equals("PROJECT") && !node.type().equals("FILE")) {
                String filePath = node.attributes().get("filePath");
                if (filePath != null) {
                    KnowledgeNode fileNode = fileNodesMap.get(filePath);
                    if (fileNode != null) {
                        addEdgeIfValid(graph, node.id(), fileNode.id(), "BELONGS_TO", nodeIdToUrnMap, existingRels);
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
                        KnowledgeNode targetNode = symbolNodesMap.get(cleanRef);
                        if (targetNode != null && !node.id().equals(targetNode.id())) {
                            addEdgeIfValid(graph, node.id(), targetNode.id(), "REFERENCES", nodeIdToUrnMap, existingRels);
                        }
                    }
                }
            }
        }
    }

    private void addEdgeIfValid(
        KnowledgeGraph graph, 
        NodeId sourceId, 
        NodeId targetId, 
        String type,
        java.util.Map<NodeId, String> urnMap,
        java.util.Set<String> existingRels
    ) {
        String sourceUrn = urnMap.getOrDefault(sourceId, "");
        String targetUrn = urnMap.getOrDefault(targetId, "");

        // Generate SHA-256-derived deterministic UUID representation using the factory
        RelationshipId relId = RelationshipIdFactory.create(sourceUrn, targetUrn, type);

        String uniqueKey = sourceId.value().toString() + "->" + targetId.value().toString() + ":" + type;
        if (existingRels.contains(relId.value().toString()) || existingRels.contains(uniqueKey)) {
            return;
        }

        if (!sourceId.equals(targetId)) {
            KnowledgeRelationship rel = new KnowledgeRelationship(relId, sourceId, targetId, type, new GraphWeight(1.0));
            graph.addRelationship(rel);
            existingRels.add(relId.value().toString());
            existingRels.add(uniqueKey);
        }
    }
}
