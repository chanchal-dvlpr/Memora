package com.contextengine.performance;

import com.contextengine.domain.entity.ContextSnapshot;
import com.contextengine.domain.valueobject.*;
import com.contextengine.application.knowledge.snapshot.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SnapshotPerformanceVerificationTest {

    @Autowired
    private SnapshotComparisonEngine comparisonEngine;

    private ContextSnapshot previousSnapshot;
    private ContextSnapshot currentSnapshot;

    @BeforeEach
    void setUp() {
        List<EngineeringEvidence> prevEvidences = new ArrayList<>();
        List<EngineeringEvidence> currEvidences = new ArrayList<>();

        // 4,000 unchanged entities (exact path and content hash match)
        for (int i = 1; i <= 4000; i++) {
            String path = "src/File" + i + ".java";
            String hash = "a".repeat(60) + String.format("%04d", i);
            EngineeringEvidence e = new EngineeringEvidence(new Path(path), 1, 10, new Hash(hash));
            prevEvidences.add(e);
            currEvidences.add(e);
        }

        // 500 modified entities (exact path match, content hash change)
        for (int i = 4001; i <= 4500; i++) {
            String path = "src/File" + i + ".java";
            String hash1 = "b".repeat(60) + String.format("%04d", i);
            String hash2 = "c".repeat(60) + String.format("%04d", i);
            prevEvidences.add(new EngineeringEvidence(new Path(path), 1, 10, new Hash(hash1)));
            currEvidences.add(new EngineeringEvidence(new Path(path), 1, 10, new Hash(hash2)));
        }

        // 200 renamed entities (different path filename, content hash match)
        for (int i = 4501; i <= 4700; i++) {
            String hash = "d".repeat(60) + String.format("%04d", i);
            prevEvidences.add(new EngineeringEvidence(new Path("src/File" + i + ".java"), 1, 10, new Hash(hash)));
            currEvidences.add(new EngineeringEvidence(new Path("src/File" + i + "-renamed.java"), 1, 10, new Hash(hash)));
        }

        // 200 moved entities (different path filename matching parent difference, content hash match)
        for (int i = 4701; i <= 4900; i++) {
            String hash = "e".repeat(60) + String.format("%04d", i);
            prevEvidences.add(new EngineeringEvidence(new Path("src/foo/File" + i + ".java"), 1, 10, new Hash(hash)));
            currEvidences.add(new EngineeringEvidence(new Path("src/bar/File" + i + ".java"), 1, 10, new Hash(hash)));
        }

        // 100 added entities (current only)
        for (int i = 4901; i <= 5000; i++) {
            String hash = "f".repeat(60) + String.format("%04d", i);
            currEvidences.add(new EngineeringEvidence(new Path("src/AddedFile" + i + ".java"), 1, 10, new Hash(hash)));
        }

        // 100 removed entities (previous only)
        for (int i = 4901; i <= 5000; i++) {
            String hash = "0".repeat(60) + String.format("%04d", i);
            prevEvidences.add(new EngineeringEvidence(new Path("src/RemovedFile" + i + ".java"), 1, 10, new Hash(hash)));
        }

        UUID projId = UUID.randomUUID();
        previousSnapshot = new ContextSnapshot(
            SnapshotId.generate(),
            new ProjectId(projId),
            new Version(1),
            Timestamp.now(),
            new ContextSummary(prevEvidences.size(), 100, Collections.emptyList()),
            prevEvidences
        );

        currentSnapshot = new ContextSnapshot(
            SnapshotId.generate(),
            new ProjectId(projId),
            new Version(2),
            Timestamp.now(),
            new ContextSummary(currEvidences.size(), 100, Collections.emptyList()),
            currEvidences
        );
    }

    @Test
    void testPerformanceMetrics() {
        SnapshotComparisonConfiguration config = new SnapshotComparisonConfiguration();
        SnapshotComparisonContext context = new SnapshotComparisonContext(
            previousSnapshot, currentSnapshot, config, "hash-scale", false
        );

        // Warm up JIT compiler
        for (int w = 0; w < 5; w++) {
            comparisonEngine.compare(context);
        }

        // 1. Comparison Latency
        long startCompare = System.currentTimeMillis();
        SnapshotComparisonResult result = comparisonEngine.compare(context);
        long compareDuration = System.currentTimeMillis() - startCompare;

        System.out.println("[PERFORMANCE-SNAPSHOT] Comparison Duration: " + compareDuration + " ms");
        assertTrue(compareDuration < 300, "Comparison should take less than 300ms, took " + compareDuration + " ms");
        assertEquals(5100, result.differences().size()); // 4000 unchanged + 500 modified + 200 renamed + 200 moved + 100 added + 100 removed = 5100

        // 2. Validation Latency
        SnapshotComparisonValidator validator = new SnapshotComparisonValidator();
        long startVal = System.currentTimeMillis();
        SnapshotComparisonValidationResult valResult = validator.validate(result.differences(), result.summary());
        long valDuration = System.currentTimeMillis() - startVal;

        System.out.println("[PERFORMANCE-SNAPSHOT] Validation Duration: " + valDuration + " ms");
        assertTrue(valDuration < 60, "Validation should take less than 60ms, took " + valDuration + " ms");
        assertTrue(valResult.isValid());

        // 3. Incremental Cache Hit Latency
        SnapshotComparisonContext cachedContext = new SnapshotComparisonContext(
            previousSnapshot, currentSnapshot, config, "hash-scale", true
        );
        long startInc = System.currentTimeMillis();
        SnapshotComparisonResult incResult = comparisonEngine.compare(cachedContext);
        long incDuration = System.currentTimeMillis() - startInc;

        System.out.println("[PERFORMANCE-SNAPSHOT] Incremental Cache Hit Duration: " + incDuration + " ms");
        assertTrue(incDuration < 50, "Incremental cache hit should be fast, took " + incDuration + " ms");
        assertEquals(result.timestamp(), incResult.timestamp());
    }

    @Test
    void testThreadSafetyAndConcurrency() throws Exception {
        SnapshotComparisonConfiguration config = new SnapshotComparisonConfiguration();
        SnapshotComparisonContext context = new SnapshotComparisonContext(previousSnapshot, currentSnapshot, config);

        int threadCount = 30;
        int runsPerThread = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<SnapshotComparisonResult>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount * runsPerThread; i++) {
            futures.add(executor.submit(() -> comparisonEngine.compare(context)));
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(60, TimeUnit.SECONDS));

        for (Future<SnapshotComparisonResult> future : futures) {
            SnapshotComparisonResult result = future.get();
            assertNotNull(result);
            assertEquals(5100, result.differences().size());
        }
    }

    @Test
    void testMemoryFootprintAndGC() {
        System.gc();
        long runtimeMemoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        SnapshotComparisonConfiguration config = new SnapshotComparisonConfiguration();
        SnapshotComparisonContext context = new SnapshotComparisonContext(previousSnapshot, currentSnapshot, config);

        for (int i = 0; i < 50; i++) {
            SnapshotComparisonResult result = comparisonEngine.compare(context);
            assertNotNull(result);
        }

        System.gc();
        long runtimeMemoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long differenceMb = (runtimeMemoryAfter - runtimeMemoryBefore) / (1024 * 1024);

        System.out.println("[PERFORMANCE-SNAPSHOT] Memory footprint difference after 50 runs: " + differenceMb + " MB");
        assertTrue(differenceMb < 25, "Possible memory leak: difference is " + differenceMb + " MB");
    }
}
