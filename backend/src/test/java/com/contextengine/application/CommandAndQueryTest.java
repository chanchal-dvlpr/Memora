package com.contextengine.application;

import com.contextengine.application.command.*;
import com.contextengine.application.query.*;
import com.contextengine.domain.service.FormatEnum;
import com.contextengine.domain.valueobject.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class CommandAndQueryTest {

    private final ProjectId projectId = ProjectId.generate();
    private final Path path = new Path("src/test");

    @Test
    void testRegisterProjectCommand() {
        RegisterProjectCommand cmd = new RegisterProjectCommand(path, "My Proj", List.of("node_modules"));
        assertThat(cmd.absoluteRootPath()).isEqualTo(path);
        assertThat(cmd.projectTitle()).isEqualTo("My Proj");
        assertThat(cmd.exclusions()).contains("node_modules");

        assertThatThrownBy(() -> new RegisterProjectCommand(null, "Title", null))
            .isInstanceOf(NullPointerException.class);
        
        assertThatThrownBy(() -> new RegisterProjectCommand(path, "  ", null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testScanProjectCommand() {
        ScanProjectCommand cmd = new ScanProjectCommand(projectId, true, false);
        assertThat(cmd.projectId()).isEqualTo(projectId);
        assertThat(cmd.deep()).isTrue();
        assertThat(cmd.shouldWait()).isFalse();
    }

    @Test
    void testGenerateContextCommand() {
        SearchQuery query = new SearchQuery("term", false, Metadata.empty(), 10);
        NodeId focus = NodeId.generate();
        TokenBudget budget = new TokenBudget(2000);

        GenerateContextCommand cmd = new GenerateContextCommand(projectId, query, focus, budget, FormatEnum.JSON);
        assertThat(cmd.projectId()).isEqualTo(projectId);
        assertThat(cmd.format()).isEqualTo(FormatEnum.JSON);
    }

    @Test
    void testCreateFeatureCommand() {
        CreateFeatureCommand cmd = new CreateFeatureCommand(projectId, "Milestone", Priority.HIGH, "details");
        assertThat(cmd.title()).isEqualTo("Milestone");
        assertThat(cmd.priority()).isEqualTo(Priority.HIGH);

        assertThatThrownBy(() -> new CreateFeatureCommand(projectId, "", Priority.HIGH, "details"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testCreateTaskCommand() {
        CreateTaskCommand cmd = new CreateTaskCommand(projectId, null, "todo description", Priority.LOW);
        assertThat(cmd.description()).isEqualTo("todo description");
        assertThat(cmd.priority()).isEqualTo(Priority.LOW);

        assertThatThrownBy(() -> new CreateTaskCommand(projectId, null, " ", Priority.LOW))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testCreateDecisionCommand() {
        CreateDecisionCommand cmd = new CreateDecisionCommand(projectId, "ADR-01", path, "rationale", "consequences");
        assertThat(cmd.title()).isEqualTo("ADR-01");
        assertThat(cmd.markdownPath()).isEqualTo(path);
    }

    @Test
    void testQueries() {
        GetProjectQuery getProj = new GetProjectQuery(projectId);
        assertThat(getProj.projectId()).isEqualTo(projectId);

        ListProjectsQuery listProj = new ListProjectsQuery(true);
        assertThat(listProj.activeOnly()).isTrue();

        GetScanStatusQuery scanStatus = new GetScanStatusQuery(projectId);
        assertThat(scanStatus.projectId()).isEqualTo(projectId);

        GetLatestSnapshotQuery latestSnapshot = new GetLatestSnapshotQuery(projectId);
        assertThat(latestSnapshot.projectId()).isEqualTo(projectId);
    }
}
