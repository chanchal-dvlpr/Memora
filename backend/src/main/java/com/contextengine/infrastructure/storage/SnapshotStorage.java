package com.contextengine.infrastructure.storage;

import com.contextengine.domain.entity.ContextSnapshot;
import java.util.Objects;

/**
 * Manages raw serialization and technical disk storage of ContextSnapshots.
 * <p>
 * Bounded Context: Context Assembly
 * Related Subsystem: Persistent Storage
 * </p>
 */
public class SnapshotStorage {

    private final LocalStorageAdapter localAdapter;

    /**
     * Constructs a SnapshotStorage.
     *
     * @param localAdapter the underlying storage adapter
     */
    public SnapshotStorage(LocalStorageAdapter localAdapter) {
        this.localAdapter = Objects.requireNonNull(localAdapter, "LocalStorageAdapter must not be null");
    }

    /**
     * Saves a context snapshot as a serialized text payload.
     *
     * @param snapshotPath path to write snapshot
     * @param snapshot the snapshot to save
     */
    public void saveSnapshot(String snapshotPath, ContextSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "Snapshot must not be null");
        String payload = "SNAPSHOT_ID=" + snapshot.id().value() + "\n" +
                         "PROJECT_ID=" + snapshot.projectId().value() + "\n" +
                         "TOKENS_USED=" + snapshot.summary().tokenFootprint() + "\n" +
                         "ENTITIES=" + String.join(",", snapshot.summary().primaryEntities()) + "\n";
        localAdapter.writeData(snapshotPath, payload);
    }
}
