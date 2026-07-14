package com.contextengine.application.scanner.language;

import com.contextengine.application.scanner.LanguageDetector;
import com.contextengine.application.scanner.SupportedLanguage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LanguageDetectorShebangTest {

    private LanguageDetector detector;

    @BeforeEach
    void setUp() {
        detector = new LanguageDetector();
    }

    private Path createTempFile(Path tempDir, String filename, String content) throws IOException {
        Path file = tempDir.resolve(filename);
        Files.writeString(file, content);
        return file;
    }

    private Path createTempBinaryFile(Path tempDir, String filename, byte[] bytes) throws IOException {
        Path file = tempDir.resolve(filename);
        Files.write(file, bytes);
        return file;
    }

    @Test
    void testPythonShebang(@TempDir Path tempDir) throws IOException {
        Path f1 = createTempFile(tempDir, "script1", "#!/usr/bin/env python\nprint('hello')\n");
        Path f2 = createTempFile(tempDir, "script2", "#!/usr/bin/python3\n");

        assertEquals(SupportedLanguage.PYTHON, detector.detect("script1", f1.toString()));
        assertEquals(SupportedLanguage.PYTHON, detector.detect("script2", f2.toString()));
    }

    @Test
    void testNodeShebang(@TempDir Path tempDir) throws IOException {
        Path f = createTempFile(tempDir, "script", "#!/usr/bin/env node\nconsole.log('hello');\n");
        assertEquals(SupportedLanguage.JAVASCRIPT, detector.detect("script", f.toString()));
    }

    @Test
    void testDenoShebang(@TempDir Path tempDir) throws IOException {
        Path f = createTempFile(tempDir, "script", "#!/usr/bin/env deno\nconsole.log('hello');\n");
        assertEquals(SupportedLanguage.TYPESCRIPT, detector.detect("script", f.toString()));
    }

    @Test
    void testBashShebang(@TempDir Path tempDir) throws IOException {
        Path f1 = createTempFile(tempDir, "script1", "#!/bin/bash\necho hello\n");
        Path f2 = createTempFile(tempDir, "script2", "#!/usr/bin/env bash\necho hello\n");

        assertEquals(SupportedLanguage.BASH, detector.detect("script1", f1.toString()));
        assertEquals(SupportedLanguage.BASH, detector.detect("script2", f2.toString()));
    }

    @Test
    void testShellShebang(@TempDir Path tempDir) throws IOException {
        Path f = createTempFile(tempDir, "script", "#!/bin/sh\necho hello\n");
        assertEquals(SupportedLanguage.SHELL, detector.detect("script", f.toString()));
    }

    @Test
    void testRubyShebang(@TempDir Path tempDir) throws IOException {
        Path f = createTempFile(tempDir, "script", "#!/usr/bin/env ruby\nputs 'hello'\n");
        assertEquals(SupportedLanguage.RUBY, detector.detect("script", f.toString()));
    }

    @Test
    void testPerlShebang(@TempDir Path tempDir) throws IOException {
        Path f = createTempFile(tempDir, "script", "#!/usr/bin/env perl\nprint 'hello';\n");
        assertEquals(SupportedLanguage.PERL, detector.detect("script", f.toString()));
    }

    @Test
    void testPhpShebang(@TempDir Path tempDir) throws IOException {
        Path f = createTempFile(tempDir, "script", "#!/usr/bin/env php\n<?php echo 'hello';\n");
        assertEquals(SupportedLanguage.PHP, detector.detect("script", f.toString()));
    }

    @Test
    void testLuaShebang(@TempDir Path tempDir) throws IOException {
        Path f = createTempFile(tempDir, "script", "#!/usr/bin/env lua\nprint('hello')\n");
        assertEquals(SupportedLanguage.LUA, detector.detect("script", f.toString()));
    }

    @Test
    void testPriorityExtensionOverShebang(@TempDir Path tempDir) throws IOException {
        // File extension .java should take priority over shebang python
        Path f = createTempFile(tempDir, "App.java", "#!/usr/bin/env python\nclass App {}\n");
        assertEquals(SupportedLanguage.JAVA, detector.detect("App.java", f.toString()));
    }

    @Test
    void testUnknownShebang(@TempDir Path tempDir) throws IOException {
        Path f = createTempFile(tempDir, "script", "#!/usr/bin/env unknown_interpreter\n");
        assertEquals(SupportedLanguage.UNSUPPORTED, detector.detect("script", f.toString()));
    }

    @Test
    void testEmptyFile(@TempDir Path tempDir) throws IOException {
        Path f = createTempFile(tempDir, "script", "");
        assertEquals(SupportedLanguage.UNSUPPORTED, detector.detect("script", f.toString()));
    }

    @Test
    void testBinaryFile(@TempDir Path tempDir) throws IOException {
        // Test shebang start with binary payload (null byte inside)
        byte[] binaryData = new byte[]{'#', '!', '/', 'b', 'i', 'n', '/', 's', 'h', '\0', 'a', 'b', 'c'};
        Path f = createTempBinaryFile(tempDir, "binary_script", binaryData);
        assertEquals(SupportedLanguage.UNSUPPORTED, detector.detect("binary_script", f.toString()));
    }
}
