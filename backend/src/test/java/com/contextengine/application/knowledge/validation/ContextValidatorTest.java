package com.contextengine.application.knowledge.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.contextengine.application.knowledge.exception.KnowledgeException;
import com.contextengine.domain.entity.ContextSnapshot;
import com.contextengine.domain.valueobject.ContextSummary;
import com.contextengine.domain.valueobject.EngineeringEvidence;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.Hash;
import com.contextengine.domain.valueobject.SnapshotId;
import com.contextengine.domain.valueobject.Timestamp;
import com.contextengine.domain.valueobject.TokenBudget;
import com.contextengine.domain.valueobject.Version;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContextValidatorTest {

    private ContextValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ContextValidator();
    }

    @Test
    void testValidate_Success() {
        ProjectId projectId = ProjectId.generate();
        ContextSnapshot snapshot = new ContextSnapshot(
            SnapshotId.generate(),
            projectId,
            new Version(1),
            Timestamp.now(),
            new ContextSummary(1, 100, List.of("urn:ce:node:test:file:app")),
            List.of(new EngineeringEvidence(
                new Path("App.java"),
                1,
                10,
                new Hash("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
            ))
        );

        assertThatCode(() -> validator.validate(snapshot, new TokenBudget(200)))
            .doesNotThrowAnyException();
    }

    @Test
    void testValidate_BudgetExceeded_Throws() {
        ProjectId projectId = ProjectId.generate();
        ContextSnapshot snapshot = new ContextSnapshot(
            SnapshotId.generate(),
            projectId,
            new Version(1),
            Timestamp.now(),
            new ContextSummary(1, 300, List.of("urn:ce:node:test:file:app")),
            List.of()
        );

        assertThatThrownBy(() -> validator.validate(snapshot, new TokenBudget(200)))
            .isInstanceOf(KnowledgeException.class)
            .hasMessageContaining("exceeds the allowed token budget");
    }

    @Test
    void testValidate_NegativeMetrics_Throws() {
        assertThatThrownBy(() -> new ContextSummary(-1, 100, List.of("urn:ce:node:test:file:app")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Total file count must be non-negative");
    }

    @Test
    void testValidate_InvalidEvidenceLines_Throws() {
        assertThatThrownBy(() -> new EngineeringEvidence(
            new Path("App.java"),
            10,
            5,
            new Hash("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("End line must be greater than or equal to start line");
    }

    @Test
    void testValidate_BlankPath_Throws() {
        ProjectId projectId = ProjectId.generate();
        ContextSnapshot snapshot = new ContextSnapshot(
            SnapshotId.generate(),
            projectId,
            new Version(1),
            Timestamp.now(),
            new ContextSummary(1, 100, List.of("urn:ce:node:test:file:app")),
            List.of(new EngineeringEvidence(
                new Path(""),
                1,
                10,
                new Hash("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
            ))
        );

        assertThatThrownBy(() -> validator.validate(snapshot, new TokenBudget(200)))
            .isInstanceOf(KnowledgeException.class)
            .hasMessageContaining("Evidence file path must not be empty");
    }

    @Test
    void testValidate_InvalidHashConstructor_Throws() {
        assertThatThrownBy(() -> new Hash("invalid-hash"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Hash value must be a valid 64-character hexadecimal string");
    }

    @Test
    void testValidate_InvalidCitationUrn_Throws() {
        ProjectId projectId = ProjectId.generate();
        ContextSnapshot snapshot = new ContextSnapshot(
            SnapshotId.generate(),
            projectId,
            new Version(1),
            Timestamp.now(),
            new ContextSummary(1, 100, List.of("invalid-urn-no-prefix")),
            List.of()
        );

        assertThatThrownBy(() -> validator.validate(snapshot, new TokenBudget(200)))
            .isInstanceOf(KnowledgeException.class)
            .hasMessageContaining("Citation entity URN must start with 'urn:' prefix");
    }
}
