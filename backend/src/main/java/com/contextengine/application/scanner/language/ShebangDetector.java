package com.contextengine.application.scanner.language;

import com.contextengine.application.scanner.SupportedLanguage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Service responsible for scanning the shebang line (first line) of files
 * and matching it against a registered pattern registry.
 */
public class ShebangDetector {

    private final List<ShebangPattern> patterns;

    /**
     * Constructs a ShebangDetector and registers the default interpreter mappings.
     */
    public ShebangDetector() {
        List<ShebangPattern> registry = new ArrayList<>();
        // Priority order matters: match more specific (e.g. bash) before generic (e.g. sh)
        registry.add(new ShebangPattern("^#!.*bin/(env\\s+)?python", SupportedLanguage.PYTHON));
        registry.add(new ShebangPattern("^#!.*bin/(env\\s+)?node", SupportedLanguage.JAVASCRIPT));
        registry.add(new ShebangPattern("^#!.*bin/(env\\s+)?deno", SupportedLanguage.TYPESCRIPT));
        registry.add(new ShebangPattern("^#!.*bin/(env\\s+)?bash", SupportedLanguage.BASH));
        registry.add(new ShebangPattern("^#!.*bin/(env\\s+)?sh\\b", SupportedLanguage.SHELL));
        registry.add(new ShebangPattern("^#!.*bin/(env\\s+)?ruby", SupportedLanguage.RUBY));
        registry.add(new ShebangPattern("^#!.*bin/(env\\s+)?perl", SupportedLanguage.PERL));
        registry.add(new ShebangPattern("^#!.*bin/(env\\s+)?php", SupportedLanguage.PHP));
        registry.add(new ShebangPattern("^#!.*bin/(env\\s+)?lua", SupportedLanguage.LUA));

        this.patterns = Collections.unmodifiableList(registry);
    }

    /**
     * Reads the first line of the file and determines its SupportedLanguage using shebang signatures.
     *
     * @param absolutePath absolute path of the file to inspect
     * @return resolved SupportedLanguage, or SupportedLanguage.UNSUPPORTED if unmatched or binary/unreadable
     */
    public SupportedLanguage detect(String absolutePath) {
        if (absolutePath == null || absolutePath.isEmpty()) {
            return SupportedLanguage.UNSUPPORTED;
        }

        File file = new File(absolutePath);
        if (!file.exists() || !file.isFile() || !file.canRead()) {
            return SupportedLanguage.UNSUPPORTED;
        }

        // Skip massive files which are likely not source script files
        if (file.length() > 10 * 1024 * 1024) { // 10MB
            return SupportedLanguage.UNSUPPORTED;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String firstLine = reader.readLine();
            if (firstLine == null || !firstLine.startsWith("#!")) {
                return SupportedLanguage.UNSUPPORTED;
            }

            // Quick binary check for control/null characters in shebang line
            for (int i = 0; i < Math.min(firstLine.length(), 200); i++) {
                char c = firstLine.charAt(i);
                if (c == '\0' || (c < 32 && c != '\n' && c != '\r' && c != '\t')) {
                    return SupportedLanguage.UNSUPPORTED;
                }
            }

            for (ShebangPattern pattern : patterns) {
                if (pattern.matches(firstLine)) {
                    return pattern.language();
                }
            }
        } catch (Exception e) {
            // Safe fallback on file read or format failure
        }

        return SupportedLanguage.UNSUPPORTED;
    }
}
