package com.contextengine.infrastructure;

import com.contextengine.domain.valueobject.Path;
import com.contextengine.infrastructure.filesystem.LocalFilesystemAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class FilesystemAdapterTest {

    private LocalFilesystemAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new LocalFilesystemAdapter();
    }

    @Test
    void testExistsAndIsDirectory(@TempDir java.nio.file.Path tempDir) {
        Path path = new Path(tempDir.toString());
        assertThat(adapter.exists(path)).isTrue();
        assertThat(adapter.isDirectory(path)).isTrue();

        Path nonExistent = new Path(tempDir.resolve("non-existent").toString());
        assertThat(adapter.exists(nonExistent)).isFalse();
    }

    @Test
    void testWriteAndReadFile(@TempDir java.nio.file.Path tempDir) throws IOException {
        java.nio.file.Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "Hello World");

        Path path = new Path(file.toString());
        assertThat(adapter.exists(path)).isTrue();
        assertThat(adapter.isDirectory(path)).isFalse();
        assertThat(adapter.readFile(path)).isEqualTo("Hello World");
    }

    @Test
    void testListFilesWithExclusions(@TempDir java.nio.file.Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("src"));
        Files.createDirectories(tempDir.resolve("target"));

        Files.writeString(tempDir.resolve("src/App.java"), "class App {}");
        Files.writeString(tempDir.resolve("target/App.class"), "class App {}");
        Files.writeString(tempDir.resolve("README.md"), "# Project");

        Path rootPath = new Path(tempDir.toString());
        List<Path> files = adapter.listFiles(rootPath, List.of("target/**", "**/*.class"));

        assertThat(files)
            .extracting(Path::value)
            .containsExactlyInAnyOrder("src/App.java", "README.md")
            .doesNotContain("target/App.class");
    }
}
