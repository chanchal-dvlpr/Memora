package com.contextengine.application.scanner;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Detects programming languages based on file extension mapping using an extensible registry.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * Dependencies: SupportedLanguage
 * Future Usage: Utilized during file discovery to attach language metadata to ScanCandidates.
 * </p>
 */
public class LanguageDetector {

    private final Map<String, SupportedLanguage> registry;
    private final com.contextengine.application.scanner.language.ShebangDetector shebangDetector;

    /**
     * Constructs a LanguageDetector and initializes the default registry mappings.
     */
    public LanguageDetector() {
        this.registry = new HashMap<>();
        this.shebangDetector = new com.contextengine.application.scanner.language.ShebangDetector();
        // Default mappings
        registry.put("java", SupportedLanguage.JAVA);
        registry.put("py", SupportedLanguage.PYTHON);
        registry.put("js", SupportedLanguage.JAVASCRIPT);
        registry.put("ts", SupportedLanguage.TYPESCRIPT);
        registry.put("cpp", SupportedLanguage.CPP);
        registry.put("h", SupportedLanguage.CPP);
        registry.put("go", SupportedLanguage.GO);
    }

    /**
     * Detects the SupportedLanguage for the given path based on its file extension.
     *
     * @param filename name or path of the file
     * @return detected SupportedLanguage, or SupportedLanguage.UNSUPPORTED if not mapped
     */
    public SupportedLanguage detect(String filename) {
        return detect(filename, null);
    }

    /**
     * Detects the SupportedLanguage based on extension with shebang line fallback.
     *
     * @param filename name or path of the file
     * @param absolutePath absolute path on host workstation for shebang inspection
     * @return detected SupportedLanguage
     */
    public SupportedLanguage detect(String filename, String absolutePath) {
        if (filename == null || filename.isEmpty()) {
            return SupportedLanguage.UNSUPPORTED;
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot != -1 && lastDot < filename.length() - 1) {
            String ext = filename.substring(lastDot + 1).toLowerCase();
            SupportedLanguage fromExt = registry.get(ext);
            if (fromExt != null && fromExt != SupportedLanguage.UNSUPPORTED) {
                return fromExt;
            }
        }

        // Fallback to shebang line check
        if (absolutePath != null) {
            SupportedLanguage fromShebang = shebangDetector.detect(absolutePath);
            if (fromShebang != SupportedLanguage.UNSUPPORTED) {
                return fromShebang;
            }
        }

        return SupportedLanguage.UNSUPPORTED;
    }

    /**
     * Registers a custom extension mapping to the detector.
     *
     * @param extension file extension (without dot)
     * @param language mapped language
     */
    public void register(String extension, SupportedLanguage language) {
        Objects.requireNonNull(extension, "Extension must not be null");
        Objects.requireNonNull(language, "Language must not be null");
        registry.put(extension.toLowerCase(), language);
    }
}
