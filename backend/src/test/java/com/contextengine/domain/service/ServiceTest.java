package com.contextengine.domain.service;

import com.contextengine.domain.entity.*;
import com.contextengine.domain.event.DomainEvent;
import com.contextengine.domain.valueobject.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class ServiceTest {

    @TempDir
    File tempDir;

    @Test
    void testProjectRegistrationAndOverlap() {
        ProjectRegistrationService registrationService = new ProjectRegistrationService();
        Path absolutePath = new Path(tempDir.getAbsolutePath());
        
        Project p1 = registrationService.registerProject(absolutePath, "Project 1", Collections.emptyList());
        assertThat(p1.title()).isEqualTo("Project 1");
        assertThat(p1.workspace()).isNotNull();

        // Registering again with overlapping directory should fail
        assertThatThrownBy(() -> registrationService.registerProject(absolutePath, "Project 2", List.of(p1)))
            .isInstanceOf(OverlappingProjectException.class);
    }

    @Test
    void testContextGenerationTokenBudgetConstraint() {
        ContextGenerationService generationService = new ContextGenerationService();
        ProjectId projectId = ProjectId.generate();
        
        KnowledgeNode n1 = new KnowledgeNode(NodeId.generate(), "CODE_SYMBOL", new Metadata(Map.of("tokens", "50")));
        KnowledgeNode n2 = new KnowledgeNode(NodeId.generate(), "CODE_SYMBOL", new Metadata(Map.of("tokens", "60")));
        
        // Total = 110, budget = 100 -> should throw BudgetUnderflowException
        assertThatThrownBy(() -> generationService.generateSnapshot(
            projectId, new Version(1), List.of(n1, n2), new TokenBudget(100), FormatEnum.MARKDOWN
        )).isInstanceOf(BudgetUnderflowException.class);
    }

    @Test
    void testKnowledgeGraphServiceReferentialIntegrity() {
        KnowledgeGraphService graphService = new KnowledgeGraphService();
        ProjectId projectId = ProjectId.generate();
        KnowledgeGraph graph = new KnowledgeGraph(projectId);
        
        NodeId sourceId = NodeId.generate();
        NodeId targetId = NodeId.generate();
        
        // Add nodes
        graph.addNode(new KnowledgeNode(sourceId, "CODE_SYMBOL", Metadata.empty()));
        graph.addNode(new KnowledgeNode(targetId, "CODE_SYMBOL", Metadata.empty()));
        
        // Add relationship
        KnowledgeRelationship relationship = new KnowledgeRelationship(
            RelationshipId.generate(), sourceId, targetId, "CALLS", new GraphWeight(1.0)
        );
        graph.addRelationship(relationship);
        
        // Validate integrity should be true
        assertThat(graphService.validateIntegrity(graph)).isTrue();
    }

    @Test
    void testDependencyAnalysisManifestParsing() throws IOException {
        DependencyAnalysisService analysisService = new DependencyAnalysisService();
        File manifestFile = new File(tempDir, "dependencies.txt");
        try (FileWriter writer = new FileWriter(manifestFile)) {
            writer.write("jackson=2.13.0\n");
            writer.write("spring-core=6.0.0\n");
        }
        
        Path manifestPath = new Path(manifestFile.getAbsolutePath());
        ProjectId projectId = ProjectId.generate();
        
        java.util.Collection<Dependency> deps = analysisService.parseDependencies(projectId, manifestPath);
        assertThat(deps).hasSize(2);
        assertThat(deps).anyMatch(d -> d.packageName().equals("jackson") && d.version().value().equals("2.13.0"));
    }

    @Test
    void testDecisionAnalysisADRParsing() throws IOException {
        DecisionAnalysisService adrService = new DecisionAnalysisService();
        File adrFile = new File(tempDir, "ADR-001.md");
        try (FileWriter writer = new FileWriter(adrFile)) {
            writer.write("# Use SQLite for metadata caching\n\nContext and decision...");
        }
        
        Path adrPath = new Path(adrFile.getAbsolutePath());
        ProjectId projectId = ProjectId.generate();
        
        Decision decision = adrService.parseADR(projectId, adrPath);
        assertThat(decision.title()).isEqualTo("Use SQLite for metadata caching");
        assertThat(decision.markdownPath()).isEqualTo(adrPath);
    }

    @Test
    void testEventProcessingQueueOverflow() {
        EventProcessingService eventService = new EventProcessingService();
        DomainEvent mockEvent = () -> Instant.now();
        
        // Queue up to 1000 events
        for (int i = 0; i < 1000; i++) {
            eventService.queueEvent(mockEvent);
        }
        
        // The 1001st event should overflow the queue
        assertThatThrownBy(() -> eventService.queueEvent(mockEvent))
            .isInstanceOf(EventQueueOverflowException.class);
    }
}
