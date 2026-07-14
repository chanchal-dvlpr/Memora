package com.contextengine.mcp.stub;

import com.contextengine.domain.entity.Context;
import com.contextengine.domain.entity.ContextSnapshot;
import com.contextengine.domain.repository.ContextRepository;
import com.contextengine.domain.valueobject.DateRange;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.SnapshotId;
import com.contextengine.domain.valueobject.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class StubContextRepository implements ContextRepository {
    @Override
    public void save(Context context) {}

    @Override
    public Optional<ContextSnapshot> findSnapshotById(SnapshotId snapshotId) {
        return Optional.empty();
    }

    @Override
    public Optional<ContextSnapshot> findLatestSnapshotForProject(ProjectId projectId) {
        return Optional.empty();
    }

    @Override
    public Collection<ContextSnapshot> findHistoryByDateRange(ProjectId projectId, DateRange range) {
        return Collections.emptyList();
    }

    @Override
    public int pruneOldSnapshots(ProjectId projectId, Timestamp retentionCutoff) {
        return 0;
    }

    @Override
    public void removeSnapshot(SnapshotId snapshotId) {}
}
