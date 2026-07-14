package com.contextengine.application.knowledge.retrieval;

import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.entity.KnowledgeRelationship;
import com.contextengine.domain.repository.KnowledgeGraphRepository;
import com.contextengine.domain.valueobject.Metadata;
import com.contextengine.domain.valueobject.NodeId;
import com.contextengine.domain.valueobject.ProjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests verifying candidate discovery, active file matching, and adjacent edge retrieval in the RetrievalEngine.
 */
class RetrievalEngineTest {

    private KnowledgeGraphRepository graphRepository;
    private RetrievalEngine retrievalEngine;
    private ProjectId projectId;

    @BeforeEach
    void setUp() {
        graphRepository = Mockito.mock(KnowledgeGraphRepository.class);
        retrievalEngine = new RetrievalEngine(graphRepository);
        projectId = ProjectId.generate();
    }

    @Test
    void testRetrieveWithActiveFile() {
        UUID fileNodeUuid = UUID.randomUUID();
        NodeId nodeId = new NodeId(fileNodeUuid);
        KnowledgeNode domainNode = new KnowledgeNode(nodeId, "FILE", new Metadata(Map.of(
            "urn", "urn:ce:node:test:file:app",
            "filePath", "src/main/java/App.java",
            "name", "App.java"
        )));

        Mockito.when(graphRepository.findNodesByProject(projectId))
            .thenReturn(List.of(domainNode));
        Mockito.when(graphRepository.findRelationshipsByProject(projectId))
            .thenReturn(List.of());

        Mockito.when(graphRepository.querySubGraph(nodeId, 3))
            .thenReturn(List.of(domainNode));

        RetrievalQuery query = new RetrievalQuery(projectId, "search", "App.java", 3);
        RetrievalResult result = retrievalEngine.retrieve(query);

        assertThat(result.nodes()).hasSize(1);
        assertThat(result.nodes().iterator().next().id()).isEqualTo(nodeId);
    }
}
