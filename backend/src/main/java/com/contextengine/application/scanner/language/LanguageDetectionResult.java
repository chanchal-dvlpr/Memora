package com.contextengine.application.scanner.language;

import com.contextengine.application.scanner.SupportedLanguage;
import java.util.Objects;

/**
 * Result record holding the resolved language and priority match source.
 */
public class LanguageDetectionResult {
    private final SupportedLanguage language;
    private final String matchSource; // "EXTENSION", "SHEBANG", or "UNKNOWN"

    /**
     * Constructs a LanguageDetectionResult.
     *
     * @param language the mapped SupportedLanguage
     * @param matchSource the match source (EXTENSION, SHEBANG, UNKNOWN)
     */
    public LanguageDetectionResult(SupportedLanguage language, String matchSource) {
        this.language = Objects.requireNonNull(language, "Language must not be null");
        this.matchSource = Objects.requireNonNull(matchSource, "MatchSource must not be null");
    }

    public SupportedLanguage language() {
        return language;
    }

    public String matchSource() {
        return matchSource;
    }
}
