package com.contextengine.application.knowledge;

import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.entity.KnowledgeGraph;
import com.contextengine.domain.repository.KnowledgeGraphRepository;
import com.contextengine.application.scanner.ScanCandidate;
import com.contextengine.application.scanner.SourceSymbol;
import com.contextengine.application.knowledge.exception.KnowledgeException;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

/**
 * Coordinates graph construction lifecycle, from initialization to validation and persistence.
 * <p>
 * Bounded Context: Knowledge Graph
 * Related Subsystem: Knowledge Graph Engine (KG-ENG)
 * Reference: Functional Requirement FR-016 (Knowledge Graph Engine) Section 9
 * </p>
 */
public class KnowledgeEngine {

    private final KnowledgeGraphRepository graphRepository;
    private final KnowledgeGraphBuilder graphBuilder;
    private final RelationshipResolver relationshipResolver;

    /**
     * Constructs a KnowledgeEngine with required dependencies.
     *
     * @param graphRepository graph repository for persistence
     * @param graphBuilder graph builder for node transformation
     * @param relationshipResolver resolver for edge connections
     */
    public KnowledgeEngine(
        KnowledgeGraphRepository graphRepository,
        KnowledgeGraphBuilder graphBuilder,
        RelationshipResolver relationshipResolver
    ) {
        this.graphRepository = Objects.requireNonNull(graphRepository, "KnowledgeGraphRepository must not be null");
        this.graphBuilder = Objects.requireNonNull(graphBuilder, "KnowledgeGraphBuilder must not be null");
        this.relationshipResolver = Objects.requireNonNull(relationshipResolver, "RelationshipResolver must not be null");
    }

    /**
     * Processes scanner candidates and symbols, assembling and persisting the Knowledge Graph.
     *
     * @param projectId target project ID
     * @param scanMode scan modality (FULL or INCREMENTAL)
     * @param candidates scanned workspace file candidates
     * @param symbols parsed code structure symbols
     * @return the constructed KnowledgeGraph aggregate root
     * @throws KnowledgeException if graph construction fails
     */
    public KnowledgeGraph buildGraph(
        ProjectId projectId,
        String scanMode,
        Collection<ScanCandidate> candidates,
        Collection<SourceSymbol> symbols
    ) {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(scanMode, "ScanMode must not be null");
        Objects.requireNonNull(candidates, "Candidates must not be null");
        Objects.requireNonNull(symbols, "Symbols must not be null");

        // Initialize Session
        KnowledgeSession session = new KnowledgeSession(UUID.randomUUID().toString());
        session.transitionTo(KnowledgeSession.State.BUILDING);

        // Initialize Context
        KnowledgeContext context = new KnowledgeContext(projectId, scanMode);

        try {
            // Load fresh or existing graph
            KnowledgeGraph graph = new KnowledgeGraph(projectId);

            // Step 1: Transform symbols and candidates into KnowledgeNode vertices
            graphBuilder.buildNodes(graph, candidates, symbols, context);

            // Transition to validating state
            session.transitionTo(KnowledgeSession.State.VALIDATING);

            // Step 2: Resolve edges / relationships
            relationshipResolver.resolve(graph, context);

            // Step 3: Persist to repository
            graphRepository.save(graph);

            // Transition to active state
            session.transitionTo(KnowledgeSession.State.ACTIVE);
            return graph;
        } catch (Exception e) {
            session.transitionTo(KnowledgeSession.State.FAULTED);
            throw new KnowledgeException(
                "Failed to construct knowledge graph for project: " + projectId.value(),
                "ERR_GRAPH_INVALID_CONFIG",
                e
            );
        }
    }
}
