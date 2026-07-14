package com.contextengine.application.scanner.hashing;

import com.contextengine.application.port.FilesystemPort;
import com.contextengine.application.scanner.ScanCandidate;
import com.contextengine.application.scanner.SupportedLanguage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StructuralHasherTest {

    private FilesystemPort mockPort;
    private StructuralHasher hasher;

    @BeforeEach
    void setUp() {
        mockPort = Mockito.mock(FilesystemPort.class);
        hasher = new StructuralHasher(mockPort);
    }

    private Path createFile(Path tempDir, String relativePath, String content) throws IOException {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }

    @Test
    void testIdenticalDirectoryStructures(@TempDir Path tempDir1, @TempDir Path tempDir2) throws IOException {
        // Build first workspace
        Path f1a = createFile(tempDir1, "src/App.java", "public class App {}");
        Path f1b = createFile(tempDir1, "pom.xml", "<project></project>");

        List<ScanCandidate> candidates1 = Arrays.asList(
            new ScanCandidate("src/App.java", f1a.toString(), 21L, Instant.now(), "FILE", SupportedLanguage.JAVA),
            new ScanCandidate("pom.xml", f1b.toString(), 19L, Instant.now(), "FILE", SupportedLanguage.UNSUPPORTED)
        );

        // Build second workspace with identical files
        Path f2a = createFile(tempDir2, "src/App.java", "public class App {}");
        Path f2b = createFile(tempDir2, "pom.xml", "<project></project>");

        List<ScanCandidate> candidates2 = Arrays.asList(
            new ScanCandidate("src/App.java", f2a.toString(), 21L, Instant.now(), "FILE", SupportedLanguage.JAVA),
            new ScanCandidate("pom.xml", f2b.toString(), 19L, Instant.now(), "FILE", SupportedLanguage.UNSUPPORTED)
        );

        StructuralHashResult result1 = hasher.calculate(tempDir1.toString(), candidates1);
        StructuralHashResult result2 = hasher.calculate(tempDir2.toString(), candidates2);

        assertEquals(result1.workspaceHash(), result2.workspaceHash());
        assertEquals(4, result1.totalNodes()); // root, src, and 2 files = 4 nodes
        assertEquals(2, result1.directoryCount()); // root and src
        assertEquals(2, result1.fileCount()); // App.java and pom.xml
    }

    @Test
    void testFileChangeChangesHashes(@TempDir Path tempDir) throws IOException {
        Path f1 = createFile(tempDir, "src/App.java", "public class App {}");
        Path f2 = createFile(tempDir, "pom.xml", "<project></project>");

        ScanCandidate c1 = new ScanCandidate("src/App.java", f1.toString(), 21L, Instant.now(), "FILE", SupportedLanguage.JAVA);
        ScanCandidate c2 = new ScanCandidate("pom.xml", f2.toString(), 19L, Instant.now(), "FILE", SupportedLanguage.UNSUPPORTED);

        String initialHash = hasher.calculate(tempDir.toString(), Arrays.asList(c1, c2)).workspaceHash();

        // Modify App.java content
        Files.writeString(f1, "public class App { // modified }");
        ScanCandidate c1Mod = new ScanCandidate("src/App.java", f1.toString(), 32L, Instant.now(), "FILE", SupportedLanguage.JAVA);

        String modifiedHash = hasher.calculate(tempDir.toString(), Arrays.asList(c1Mod, c2)).workspaceHash();

        assertNotEquals(initialHash, modifiedHash);
    }

    @Test
    void testFileRenameChangesHashes(@TempDir Path tempDir) throws IOException {
        Path f1 = createFile(tempDir, "src/App.java", "public class App {}");
        ScanCandidate c1 = new ScanCandidate("src/App.java", f1.toString(), 21L, Instant.now(), "FILE", SupportedLanguage.JAVA);

        String initialHash = hasher.calculate(tempDir.toString(), Collections.singletonList(c1)).workspaceHash();

        // Rename App.java to Main.java
        Path f2 = tempDir.resolve("src/Main.java");
        Files.move(f1, f2);
        ScanCandidate c2 = new ScanCandidate("src/Main.java", f2.toString(), 21L, Instant.now(), "FILE", SupportedLanguage.JAVA);

        String renamedHash = hasher.calculate(tempDir.toString(), Collections.singletonList(c2)).workspaceHash();

        assertNotEquals(initialHash, renamedHash);
    }

    @Test
    void testDirectoryRenameChangesHashes(@TempDir Path tempDir) throws IOException {
        Path f1 = createFile(tempDir, "src/App.java", "public class App {}");
        ScanCandidate c1 = new ScanCandidate("src/App.java", f1.toString(), 21L, Instant.now(), "FILE", SupportedLanguage.JAVA);

        String initialHash = hasher.calculate(tempDir.toString(), Collections.singletonList(c1)).workspaceHash();

        // Rename directory "src" to "source"
        Path srcDir = tempDir.resolve("src");
        Path sourceDir = tempDir.resolve("source");
        Files.move(srcDir, sourceDir);
        Path f2 = sourceDir.resolve("App.java");
        ScanCandidate c2 = new ScanCandidate("source/App.java", f2.toString(), 21L, Instant.now(), "FILE", SupportedLanguage.JAVA);

        String renamedHash = hasher.calculate(tempDir.toString(), Collections.singletonList(c2)).workspaceHash();

        assertNotEquals(initialHash, renamedHash);
    }

    @Test
    void testEmptyAndNestedDirectories(@TempDir Path tempDir) throws IOException {
        // Workspace containing an empty directory node candidate
        ScanCandidate dirCandidate = new ScanCandidate("empty_dir", tempDir.resolve("empty_dir").toString(), 0L, Instant.now(), "DIR", SupportedLanguage.UNSUPPORTED);
        Files.createDirectories(tempDir.resolve("empty_dir"));

        StructuralHashResult result = hasher.calculate(tempDir.toString(), Collections.singletonList(dirCandidate));
        assertEquals(2, result.totalNodes()); // root and empty_dir
        assertEquals(2, result.directoryCount());
        assertEquals(0, result.fileCount());
    }

    @Test
    void testDeterministicOrdering(@TempDir Path tempDir) throws IOException {
        Path f1 = createFile(tempDir, "a.java", "class A {}");
        Path f2 = createFile(tempDir, "b.java", "class B {}");

        ScanCandidate c1 = new ScanCandidate("a.java", f1.toString(), 10L, Instant.now(), "FILE", SupportedLanguage.JAVA);
        ScanCandidate c2 = new ScanCandidate("b.java", f2.toString(), 10L, Instant.now(), "FILE", SupportedLanguage.JAVA);

        // Compute hash with order (c1, c2)
        String hashA = hasher.calculate(tempDir.toString(), Arrays.asList(c1, c2)).workspaceHash();

        // Compute hash with order (c2, c1)
        String hashB = hasher.calculate(tempDir.toString(), Arrays.asList(c2, c1)).workspaceHash();

        assertEquals(hashA, hashB);
    }

    @Test
    void testHashCacheReuse(@TempDir Path tempDir) throws IOException {
        Path f1 = createFile(tempDir, "App.java", "class App {}");
        Instant lastMod = Instant.now();
        ScanCandidate c1 = new ScanCandidate("App.java", f1.toString(), 12L, lastMod, "FILE", SupportedLanguage.JAVA);

        // Initial calculation calculates and caches
        String hash1 = hasher.calculate(tempDir.toString(), Collections.singletonList(c1)).workspaceHash();

        // Delete the physical file so that if the hasher reads the file again, it would fail
        Files.delete(f1);

        // Compute again with identical parameters: should reuse the cached file hash and succeed
        String hash2 = hasher.calculate(tempDir.toString(), Collections.singletonList(c1)).workspaceHash();

        assertEquals(hash1, hash2);
    }
}
