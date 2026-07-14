package com.contextengine.application.knowledge.builder;

import com.contextengine.application.knowledge.ranking.ContextRankedResult;
import com.contextengine.domain.entity.ContextSnapshot;
import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.valueobject.Metadata;
import com.contextengine.domain.valueobject.NodeId;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests verifying ContextSnapshot assembly, summary metrics, and engineering evidence lists in ContextBuilder.
 */
class ContextBuilderTest {

    private ContextBuilder contextBuilder;

    @BeforeEach
    void setUp() {
        contextBuilder = new ContextBuilder();
    }

    @Test
    void testBuildContextSnapshot() {
        ProjectId projectId = ProjectId.generate();
        NodeId nodeId = NodeId.generate();

        KnowledgeNode node = new KnowledgeNode(nodeId, "FILE", new Metadata(Map.of(
            "urn", "urn:ce:node:test:file:main",
            "filePath", "src/Main.java",
            "name", "Main.java",
            "tokens", "150",
            "startLine", "10",
            "endLine", "20"
        )));

        ContextRankedResult ranked = new ContextRankedResult(node, 0.95);
        ContextSnapshot snapshot = contextBuilder.build(projectId, new Version(1), Collections.singletonList(ranked));

        assertThat(snapshot.projectId()).isEqualTo(projectId);
        assertThat(snapshot.version().value()).isEqualTo(1);
        assertThat(snapshot.summary().totalFileCount()).isEqualTo(1);
        assertThat(snapshot.summary().tokenFootprint()).isEqualTo(150);
        assertThat(snapshot.summary().primaryEntities()).contains("urn:ce:node:test:file:main");
        assertThat(snapshot.evidences()).hasSize(1);
        assertThat(snapshot.evidences().get(0).filePath().value()).isEqualTo("src/Main.java");
        assertThat(snapshot.evidences().get(0).startLine()).isEqualTo(10);
        assertThat(snapshot.evidences().get(0).endLine()).isEqualTo(20);
    }
}
