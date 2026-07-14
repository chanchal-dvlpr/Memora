package com.contextengine.application.knowledge.snapshot;

import com.contextengine.domain.entity.ContextSnapshot;
import com.contextengine.domain.valueobject.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SnapshotComparisonEngineTest {

    @Autowired
    private SnapshotComparisonEngine comparisonEngine;

    @Test
    void testSpringBeanRegistration() {
        assertNotNull(comparisonEngine);
    }

    private ContextSnapshot createSnapshot(List<EngineeringEvidence> evidences) {
        return new ContextSnapshot(
            SnapshotId.generate(),
            new ProjectId(UUID.randomUUID()),
            new Version(1),
            Timestamp.now(),
            new ContextSummary(evidences.size(), 100, Collections.emptyList()),
            evidences
        );
    }

    @Test
    void testIdenticalSnapshots() {
        EngineeringEvidence ev1 = new EngineeringEvidence(new Path("src/File1.java"), 1, 10, new Hash("1".repeat(64)));
        EngineeringEvidence ev2 = new EngineeringEvidence(new Path("src/File2.java"), 1, 20, new Hash("2".repeat(64)));

        ContextSnapshot prev = createSnapshot(List.of(ev1, ev2));
        ContextSnapshot curr = createSnapshot(List.of(ev1, ev2));

        SnapshotComparisonContext context = new SnapshotComparisonContext(prev, curr, new SnapshotComparisonConfiguration());
        SnapshotComparisonResult result = comparisonEngine.compare(context);

        assertEquals(2, result.summary().unchanged());
        assertEquals(0, result.summary().added());
        assertEquals(0, result.summary().removed());
        assertEquals(2, result.statistics().entitiesCompared());
        assertEquals(0, result.statistics().differencesFound());
    }

    @Test
    void testAddedAndRemovedEntities() {
        EngineeringEvidence ev1 = new EngineeringEvidence(new Path("src/File1.java"), 1, 10, new Hash("1".repeat(64)));
        EngineeringEvidence ev2 = new EngineeringEvidence(new Path("src/File2.java"), 1, 20, new Hash("2".repeat(64)));
        EngineeringEvidence ev3 = new EngineeringEvidence(new Path("src/File3.java"), 1, 30, new Hash("3".repeat(64)));

        ContextSnapshot prev = createSnapshot(List.of(ev1, ev2));
        ContextSnapshot curr = createSnapshot(List.of(ev2, ev3));

        SnapshotComparisonContext context = new SnapshotComparisonContext(prev, curr, new SnapshotComparisonConfiguration());
        SnapshotComparisonResult result = comparisonEngine.compare(context);

        assertEquals(1, result.summary().unchanged());
        assertEquals(1, result.summary().added());
        assertEquals(1, result.summary().removed());
        assertEquals(2, result.statistics().entitiesCompared());
        assertEquals(2, result.statistics().differencesFound());
    }

    @Test
    void testModifiedEntities() {
        EngineeringEvidence ev1Prev = new EngineeringEvidence(new Path("src/File1.java"), 1, 10, new Hash("1".repeat(64)));
        EngineeringEvidence ev1Curr = new EngineeringEvidence(new Path("src/File1.java"), 1, 10, new Hash("f".repeat(64)));

        ContextSnapshot prev = createSnapshot(List.of(ev1Prev));
        ContextSnapshot curr = createSnapshot(List.of(ev1Curr));

        SnapshotComparisonContext context = new SnapshotComparisonContext(prev, curr, new SnapshotComparisonConfiguration());
        SnapshotComparisonResult result = comparisonEngine.compare(context);

        assertEquals(1, result.summary().modified());
        assertEquals(0, result.summary().unchanged());
        assertEquals(1, result.statistics().differencesFound());
    }

    @Test
    void testRenamedEntities() {
        EngineeringEvidence evPrev = new EngineeringEvidence(new Path("src/File1.java"), 1, 10, new Hash("a".repeat(64)));
        EngineeringEvidence evCurr = new EngineeringEvidence(new Path("src/File1-renamed.java"), 1, 10, new Hash("a".repeat(64)));

        ContextSnapshot prev = createSnapshot(List.of(evPrev));
        ContextSnapshot curr = createSnapshot(List.of(evCurr));

        SnapshotComparisonContext context = new SnapshotComparisonContext(prev, curr, new SnapshotComparisonConfiguration());
        SnapshotComparisonResult result = comparisonEngine.compare(context);

        assertEquals(1, result.summary().renamed());
        assertEquals(0, result.summary().added());
        assertEquals(0, result.summary().removed());
        assertEquals(1, result.statistics().differencesFound());
    }

    @Test
    void testMovedEntities() {
        EngineeringEvidence evPrev = new EngineeringEvidence(new Path("src/foo/File1.java"), 1, 10, new Hash("a".repeat(64)));
        EngineeringEvidence evCurr = new EngineeringEvidence(new Path("src/bar/File1.java"), 1, 10, new Hash("a".repeat(64)));

        ContextSnapshot prev = createSnapshot(List.of(evPrev));
        ContextSnapshot curr = createSnapshot(List.of(evCurr));

        SnapshotComparisonContext context = new SnapshotComparisonContext(prev, curr, new SnapshotComparisonConfiguration());
        SnapshotComparisonResult result = comparisonEngine.compare(context);

        assertEquals(1, result.summary().moved());
        assertEquals(0, result.summary().added());
        assertEquals(0, result.summary().removed());
        assertEquals(1, result.statistics().differencesFound());
    }

    @Test
    void testValidatorFailures() {
        SnapshotComparisonValidator validator = new SnapshotComparisonValidator();

        EngineeringEvidence ev1 = new EngineeringEvidence(new Path("src/File1.java"), 1, 10, new Hash("1".repeat(64)));
        EngineeringEvidence ev2 = new EngineeringEvidence(new Path("src/File2.java"), 1, 10, new Hash("2".repeat(64)));

        // 1. Inconsistent count validation check
        List<SnapshotDifference> differences = List.of(
            new SnapshotDifference(SnapshotDifferenceType.ADDED, null, ev1, "Added")
        );
        SnapshotComparisonSummary badSummary = new SnapshotComparisonSummary(0, 0, 0, 0, 0, 0); // expects 0 added, but we have 1
        SnapshotComparisonValidationResult countResult = validator.validate(differences, badSummary);
        assertFalse(countResult.isValid());
        assertTrue(countResult.errors().stream().anyMatch(e -> e.contains("Inconsistent ADDED summary count")));

        // 2. Duplicate difference validation check
        List<SnapshotDifference> duplicateDiffs = List.of(
            new SnapshotDifference(SnapshotDifferenceType.MODIFIED, ev1, ev2, "Modified 1"),
            new SnapshotDifference(SnapshotDifferenceType.MODIFIED, ev1, ev2, "Modified 2")
        );
        SnapshotComparisonSummary summary = new SnapshotComparisonSummary(0, 0, 2, 0, 0, 0);
        SnapshotComparisonValidationResult duplicateResult = validator.validate(duplicateDiffs, summary);
        assertFalse(duplicateResult.isValid());
        assertTrue(duplicateResult.errors().stream().anyMatch(e -> e.contains("Duplicate previous evidence path reference")));

        // 3. Rename with mismatched hash check
        List<SnapshotDifference> mismatchedRename = List.of(
            new SnapshotDifference(SnapshotDifferenceType.RENAMED, ev1, ev2, "Renamed")
        );
        SnapshotComparisonSummary renameSummary = new SnapshotComparisonSummary(0, 0, 0, 1, 0, 0);
        SnapshotComparisonValidationResult renameResult = validator.validate(mismatchedRename, renameSummary);
        assertFalse(renameResult.isValid());
        assertTrue(renameResult.errors().stream().anyMatch(e -> e.contains("RENAMED difference at index 0 has mismatched hashes")));
    }

    @Test
    void testIncrementalCacheReuse() {
        EngineeringEvidence ev1 = new EngineeringEvidence(new Path("src/File1.java"), 1, 10, new Hash("1".repeat(64)));
        ContextSnapshot prev = createSnapshot(List.of(ev1));
        ContextSnapshot curr = createSnapshot(List.of(ev1));

        // 1. Initial run
        SnapshotComparisonContext initialContext = new SnapshotComparisonContext(
            prev, curr, new SnapshotComparisonConfiguration(), "hash-1", false
        );
        SnapshotComparisonResult result1 = comparisonEngine.compare(initialContext);
        assertNotNull(result1);

        // 2. Unchanged structural hash -> complete cache hit
        SnapshotComparisonContext cachedContext = new SnapshotComparisonContext(
            prev, curr, new SnapshotComparisonConfiguration(), "hash-1", true
        );
        SnapshotComparisonResult result2 = comparisonEngine.compare(cachedContext);
        assertEquals(result1.timestamp(), result2.timestamp()); // Confirms complete cache hit!
    }
}
