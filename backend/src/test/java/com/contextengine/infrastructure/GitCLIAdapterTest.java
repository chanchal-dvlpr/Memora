package com.contextengine.infrastructure;

import com.contextengine.domain.valueobject.Path;
import com.contextengine.infrastructure.git.GitCLIAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import static org.assertj.core.api.Assertions.assertThat;

class GitCLIAdapterTest {

    private GitCLIAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new GitCLIAdapter();
    }

    @Test
    void testNonGitDirectory(@TempDir java.nio.file.Path tempDir) {
        Path path = new Path(tempDir.toString());
        assertThat(adapter.isGitRepository(path)).isFalse();
        assertThat(adapter.getActiveBranch(path)).isEmpty();
        assertThat(adapter.getLatestCommitHash(path)).isEmpty();
    }

    @Test
    void testGitDirectoryDetection(@TempDir java.nio.file.Path tempDir) throws IOException {
        java.nio.file.Path gitDir = tempDir.resolve(".git");
        java.nio.file.Files.createDirectory(gitDir);
        Path path = new Path(tempDir.toString());
        assertThat(adapter.isGitRepository(path)).isTrue();
    }
}
