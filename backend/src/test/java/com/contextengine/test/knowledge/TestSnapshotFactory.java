package com.contextengine.test.knowledge;

import com.contextengine.domain.entity.ContextSnapshot;
import com.contextengine.domain.valueobject.*;
import java.util.*;

/**
 * Reusable test factory generating test ContextSnapshot instances.
 */
public class TestSnapshotFactory {

    public static ContextSnapshot createSnapshot(String projectIdStr) {
        SnapshotId id = SnapshotId.generate();
        ProjectId projectId = new ProjectId(UUID.fromString(projectIdStr));
        Version version = new Version(1);
        Timestamp createdAt = new Timestamp(java.time.Instant.now());
        ContextSummary summary = new ContextSummary(1, 100, List.of("Main.java"));
        List<EngineeringEvidence> evidences = List.of(
            new EngineeringEvidence(new Path("/workspace/src/Main.java"), 1, 10, new Hash("hash"))
        );
        return new ContextSnapshot(id, projectId, version, createdAt, summary, evidences);
    }
}
