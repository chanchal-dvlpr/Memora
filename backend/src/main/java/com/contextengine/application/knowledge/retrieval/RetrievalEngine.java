package com.contextengine.application.knowledge.retrieval;

import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.entity.KnowledgeRelationship;
import com.contextengine.domain.repository.KnowledgeGraphRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service orchestrating the retrieval of relevant context subgraphs.
 * Locates entry seed nodes based on queries or active paths, and gathers adjacent edges.
 * <p>
 * Bounded Context: Knowledge Graph / Context Assembly
 * Architecture Reference: Functional Requirement FR-014 (Context Generation Engine) Section 8 (Lifecycle state: COLLECTED)
 * Responsibility: Traverses and retrieves candidate nodes and relationships for ranking.
 * Dependencies: {@link KnowledgeGraphRepository}, {@link SpringDataKnowledgeNodeRepository}, {@link SpringDataKnowledgeRelationshipRepository}.
 * Future Usage: Integration with vector similarity retrieval engines.
 * </p>
 */
public class RetrievalEngine {

    private final KnowledgeGraphRepository graphRepository;

    /**
     * Constructs a RetrievalEngine.
     *
     * @param graphRepository the knowledge graph repository
     */
    public RetrievalEngine(KnowledgeGraphRepository graphRepository) {
        this.graphRepository = Objects.requireNonNull(graphRepository, "KnowledgeGraphRepository must not be null");
    }

    /**
     * Executes knowledge candidate retrieval for a project based on active files and search terms.
     *
     * @param query the retrieval query specifications
     * @return the gathered nodes and matching relationships
     */
    public RetrievalResult retrieve(RetrievalQuery query) {
        Objects.requireNonNull(query, "Query must not be null");

        // 1. Fetch all nodes in the project to locate seed nodes using the abstraction
        Collection<KnowledgeNode> allProjectNodes = graphRepository.findNodesByProject(query.projectId());

        if (allProjectNodes.isEmpty()) {
            return new RetrievalResult(new ArrayList<>(), new ArrayList<>());
        }

        // 2. Discover seed nodes
        Set<KnowledgeNode> seeds = new HashSet<>();

        // Match active file path if provided
        if (query.activeFilePath() != null && !query.activeFilePath().isBlank()) {
            allProjectNodes.stream()
                .filter(n -> n.type().equalsIgnoreCase("FILE"))
                .filter(n -> {
                    String qname = n.attributes().get("qualifiedName");
                    if (qname == null) {
                        qname = n.attributes().get("name");
                    }
                    return (qname != null && qname.endsWith(query.activeFilePath())) ||
                           (n.attributes().get("filePath") != null && n.attributes().get("filePath").equals(query.activeFilePath()));
                })
                .forEach(seeds::add);
        }

        // Match search terms if query focus keywords are specified
        if (query.contextQuery() != null && !query.contextQuery().isBlank()) {
            String[] terms = query.contextQuery().toLowerCase().split("\\s+");
            for (KnowledgeNode node : allProjectNodes) {
                String qname = node.attributes().get("qualifiedName");
                if (qname == null) {
                    qname = node.attributes().get("name");
                }
                String nameVal = qname != null ? qname.toLowerCase() : "";
                String typeVal = node.type() != null ? node.type().toLowerCase() : "";
                String docstringVal = node.attributes().get("docstring") != null ? node.attributes().get("docstring").toLowerCase() : "";

                for (String term : terms) {
                    if (!term.isBlank() && 
                        (nameVal.contains(term) ||
                         typeVal.contains(term) ||
                         docstringVal.contains(term))) {
                        seeds.add(node);
                        break;
                    }
                }
            }
        }

        // If no seeds could be found, fall back to any PROJECT type node
        if (seeds.isEmpty()) {
            allProjectNodes.stream()
                .filter(n -> n.type().equalsIgnoreCase("PROJECT"))
                .forEach(seeds::add);
        }

        // If still empty, fall back to first node
        if (seeds.isEmpty()) {
            seeds.add(allProjectNodes.iterator().next());
        }

        // 3. Query subgraphs for each seed node
        Set<KnowledgeNode> retrievedNodes = new HashSet<>();
        for (KnowledgeNode seed : seeds) {
            Collection<KnowledgeNode> subGraphNodes = graphRepository.querySubGraph(seed.id(), query.depthLimit());
            retrievedNodes.addAll(subGraphNodes);
        }

        // Always ensure seed nodes themselves are included
        retrievedNodes.addAll(seeds);

        // 4. Retrieve connecting relationships for referential integrity using the abstraction
        Collection<KnowledgeRelationship> allRelationships = graphRepository.findRelationshipsByProject(query.projectId());

        Set<KnowledgeRelationship> retrievedRelationships = new HashSet<>();
        Set<com.contextengine.domain.valueobject.NodeId> nodeIds = retrievedNodes.stream()
            .map(KnowledgeNode::id)
            .collect(Collectors.toSet());

        for (KnowledgeRelationship relationship : allRelationships) {
            if (nodeIds.contains(relationship.sourceNodeId()) && nodeIds.contains(relationship.targetNodeId())) {
                retrievedRelationships.add(relationship);
            }
        }

        return new RetrievalResult(retrievedNodes, retrievedRelationships);
    }
}
