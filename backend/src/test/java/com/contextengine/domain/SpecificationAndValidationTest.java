package com.contextengine.domain;

import com.contextengine.domain.entity.ContextSnapshot;
import com.contextengine.domain.entity.Decision;
import com.contextengine.domain.entity.Dependency;
import com.contextengine.domain.entity.Project;
import com.contextengine.domain.specification.*;
import com.contextengine.domain.validation.*;
import com.contextengine.domain.valueobject.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class SpecificationAndValidationTest {

    @TempDir
    File tempDir;

    @Test
    void testTokenBudgetSpecification() {
        TokenBudget budget = new TokenBudget(100);
        TokenBudgetSpecification spec = new TokenBudgetSpecification(budget);

        ContextSummary summaryValid = new ContextSummary(2, 80, List.of("Entity1", "Entity2"));
        ContextSnapshot snapshotValid = new ContextSnapshot(
            SnapshotId.generate(), ProjectId.generate(), new Version(1), Timestamp.now(), summaryValid, Collections.emptyList()
        );

        ContextSummary summaryInvalid = new ContextSummary(2, 120, List.of("Entity1", "Entity2"));
        ContextSnapshot snapshotInvalid = new ContextSnapshot(
            SnapshotId.generate(), ProjectId.generate(), new Version(1), Timestamp.now(), summaryInvalid, Collections.emptyList()
        );

        assertThat(spec.isSatisfiedBy(snapshotValid)).isTrue();
        assertThat(spec.isSatisfiedBy(snapshotInvalid)).isFalse();
    }

    @Test
    void testSemVerComplianceSpecification() {
        SemVerComplianceSpecification spec = new SemVerComplianceSpecification();

        assertThat(spec.isSatisfiedBy(new SemanticVersion("1.0.0"))).isTrue();
        assertThat(spec.isSatisfiedBy(new SemanticVersion("2.14.0-rc1"))).isTrue();
        
        assertThatThrownBy(() -> new SemanticVersion("invalid-version"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testSelfSupersedePreventionSpecification() {
        SelfSupersedePreventionSpecification spec = new SelfSupersedePreventionSpecification();
        ProjectId projectId = ProjectId.generate();
        Path adrPath = new Path("src/test/adr.md");
        DecisionId decisionId = DecisionId.generate();

        Decision validDecision = new Decision(decisionId, projectId, "Decision 1", adrPath);
        Decision invalidDecision = new Decision(decisionId, projectId, "Decision 2", adrPath);
        
        try {
            java.lang.reflect.Field field = Decision.class.getDeclaredField("supersededBy");
            field.setAccessible(true);
            field.set(invalidDecision, decisionId);
        } catch (Exception e) {
            fail("Failed to set supersededBy field via reflection: " + e.getMessage());
        }

        assertThat(spec.isSatisfiedBy(validDecision)).isTrue();
        assertThat(spec.isSatisfiedBy(invalidDecision)).isFalse();
    }

    @Test
    void testProjectValidator() {
        File existingFolder = new File(tempDir, "proj1");
        existingFolder.mkdirs();

        Path path = new Path(existingFolder.getAbsolutePath());
        Project project = new Project(ProjectId.generate(), path, "Project 1");
        
        ProjectValidator validator = new ProjectValidator(Collections.emptyList());
        
        // Should pass
        assertThatCode(() -> validator.validate(project)).doesNotThrowAnyException();

        // Test duplicate overlap
        Project project2 = new Project(ProjectId.generate(), path, "Project 2");
        ProjectValidator validatorOverlap = new ProjectValidator(List.of(project));
        
        assertThatThrownBy(() -> validatorOverlap.validate(project2))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Overlapping project directory detected");
    }

    @Test
    void testDependencyValidator() {
        DependencyValidator validator = new DependencyValidator();
        ProjectId projectId = ProjectId.generate();
        Path manifestPath = new Path("package.json");

        Dependency validDep = new Dependency(
            DependencyId.generate(), projectId, "lib", new SemanticVersion("1.0.0"), manifestPath
        );

        assertThatCode(() -> validator.validate(validDep)).doesNotThrowAnyException();
        
        assertThatThrownBy(() -> new SemanticVersion("1.invalid"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
