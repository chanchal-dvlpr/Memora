package com.contextengine.domain.entity;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Represents a transactional batch of node and relationship modifications to commit to the Knowledge Graph.
 */
public class GraphTransaction {
    
    private final Collection<KnowledgeNode> nodesToSave;
    private final Collection<KnowledgeRelationship> relationshipsToSave;

    /**
     * Constructs a GraphTransaction batch.
     *
     * @param nodesToSave nodes to save/update
     * @param relationshipsToSave relationships to save/update
     */
    public GraphTransaction(Collection<KnowledgeNode> nodesToSave, Collection<KnowledgeRelationship> relationshipsToSave) {
        this.nodesToSave = Collections.unmodifiableCollection(Objects.requireNonNull(nodesToSave, "Nodes to save must not be null"));
        this.relationshipsToSave = Collections.unmodifiableCollection(Objects.requireNonNull(relationshipsToSave, "Relationships to save must not be null"));
    }

    public Collection<KnowledgeNode> nodesToSave() {
        return nodesToSave;
    }

    public Collection<KnowledgeRelationship> relationshipsToSave() {
        return relationshipsToSave;
    }
}
