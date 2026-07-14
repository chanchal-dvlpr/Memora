package com.contextengine.domain.valueobject;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.contextengine.test.BaseUnitTest;
import static org.assertj.core.api.Assertions.*;

class ValueObjectTest extends BaseUnitTest {

    @Test
    void testIdentifiersUuidV4Validation() {
        UUID v4Uuid = UUID.randomUUID();
        ProjectId projectId = new ProjectId(v4Uuid);
        assertThat(projectId.value()).isEqualTo(v4Uuid);

        // Enforce that a non-v4 UUID (such as a namespace UUID v1/v5) throws IllegalArgumentException
        UUID v1Uuid = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");
        assertThatThrownBy(() -> new ProjectId(v1Uuid))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("version 4");
    }

    @Test
    void testVersionPositiveRequirement() {
        Version version = new Version(1);
        assertThat(version.value()).isEqualTo(1);
        assertThat(version.next().value()).isEqualTo(2);

        assertThatThrownBy(() -> new Version(0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testSemanticVersionRegex() {
        SemanticVersion semVer = new SemanticVersion("1.0.0-rc1+meta");
        assertThat(semVer.value()).isEqualTo("1.0.0-rc1+meta");

        assertThatThrownBy(() -> new SemanticVersion("invalid-version"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testPathBoundaryEscapeRejection() {
        Path path = new Path("src/main/java");
        assertThat(path.value()).isEqualTo("src/main/java");

        assertThatThrownBy(() -> new Path("../escape"))
            .isInstanceOf(IllegalArgumentException.class);
        
        assertThatThrownBy(() -> new Path("sub/../../escape"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testHashSha256HexSchema() {
        String validHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        Hash hash = new Hash(validHash.toUpperCase());
        // Value object must normalize hash to lowercase
        assertThat(hash.value()).isEqualTo(validHash);

        assertThatThrownBy(() -> new Hash("short-hash"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testTimestampMicrosecondTruncation() {
        Instant now = Instant.now();
        Timestamp timestamp = new Timestamp(now);
        // Verify nanoseconds part is truncated to microsecond bounds
        assertThat(timestamp.value().getNano() % 1000).isZero();
    }

    @Test
    void testDateRangeChronologicalOrder() {
        Timestamp start = Timestamp.now();
        Timestamp end = new Timestamp(start.value().plusSeconds(10));
        DateRange dateRange = new DateRange(start, end);
        assertThat(dateRange.contains(start)).isTrue();

        assertThatThrownBy(() -> new DateRange(end, start))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testConfidenceScoreBoundariesAndEpsilonEquality() {
        ConfidenceScore score1 = new ConfidenceScore(0.85000001);
        ConfidenceScore score2 = new ConfidenceScore(0.85000002);
        // Equal within the 10^-7 epsilon tolerance
        assertThat(score1).isEqualTo(score2);

        assertThatThrownBy(() -> new ConfidenceScore(1.1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testMetadataImmutability() {
        Metadata metadata = new Metadata(Map.of("key", "value"));
        assertThat(metadata.get("key")).isEqualTo("value");
        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> metadata.values().put("another", "one"));
    }

    @Test
    void testContextSummaryNonNegativeValidation() {
        ContextSummary summary = new ContextSummary(5, 120, List.of("Project", "Module"));
        assertThat(summary.totalFileCount()).isEqualTo(5);
        assertThat(summary.tokenFootprint()).isEqualTo(120);

        assertThatThrownBy(() -> new ContextSummary(-1, 100, List.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testAIConfidenceValidation() {
        AIConfidence confidence = new AIConfidence(0.95, List.of("Log entry"));
        assertThat(confidence.score()).isEqualTo(0.95);

        assertThatThrownBy(() -> new AIConfidence(1.05, List.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testEngineeringEvidenceValidation() {
        Path path = new Path("src/file.java");
        Hash hash = new Hash("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        EngineeringEvidence evidence = new EngineeringEvidence(path, 10, 20, hash);
        
        assertThat(evidence.filePath()).isEqualTo(path);
        assertThat(evidence.startLine()).isEqualTo(10);
        assertThat(evidence.endLine()).isEqualTo(20);

        assertThatThrownBy(() -> new EngineeringEvidence(path, 20, 10, hash))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testSearchQueryValidation() {
        SearchQuery query = new SearchQuery("testTerm", true, Metadata.empty(), 10);
        assertThat(query.term()).isEqualTo("testTerm");

        assertThatThrownBy(() -> new SearchQuery("   ", true, Metadata.empty(), 10))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testFilterCriteria() {
        FilterCriteria filter = new FilterCriteria(List.of(new Path("ex")), List.of("java"), Priority.MEDIUM);
        assertThat(filter.minimumPriority()).isEqualTo(Priority.MEDIUM);
    }
}
