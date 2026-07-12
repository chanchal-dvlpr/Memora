package com.contextengine.infrastructure;

import com.contextengine.domain.entity.ContextSnapshot;
import com.contextengine.domain.valueobject.*;
import com.contextengine.infrastructure.storage.LocalStorageAdapter;
import com.contextengine.infrastructure.storage.SnapshotStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import static org.assertj.core.api.Assertions.assertThat;

class StorageInfrastructureTest {

    @Test
    void testLocalStorageAdapter(@TempDir java.nio.file.Path tempDir) {
        LocalStorageAdapter adapter = new LocalStorageAdapter();
        String file = tempDir.resolve("test.txt").toString();

        adapter.writeData(file, "hello disk");
        assertThat(adapter.readData(file)).isEqualTo("hello disk");
    }

    @Test
    void testSnapshotStorage(@TempDir java.nio.file.Path tempDir) throws IOException {
        LocalStorageAdapter localAdapter = new LocalStorageAdapter();
        SnapshotStorage storage = new SnapshotStorage(localAdapter);
        String snapshotFile = tempDir.resolve("snapshot.txt").toString();

        ContextSnapshot snapshot = new ContextSnapshot(
            SnapshotId.generate(),
            ProjectId.generate(),
            new Version(1),
            Timestamp.now(),
            new ContextSummary(1, 100, Collections.emptyList()),
            Collections.emptyList()
        );

        storage.saveSnapshot(snapshotFile, snapshot);
        String savedContent = Files.readString(tempDir.resolve("snapshot.txt"));
        assertThat(savedContent).contains("SNAPSHOT_ID=").contains("TOKENS_USED=100");
    }
}
