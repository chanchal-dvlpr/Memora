package com.contextengine.application.scanner.language;

import com.contextengine.application.scanner.SupportedLanguage;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable mapping representing a compiled shebang regex pattern and its target language.
 */
public class ShebangPattern {
    private final Pattern pattern;
    private final SupportedLanguage language;

    /**
     * Constructs a ShebangPattern.
     *
     * @param regex regex mapping of shebang interpreter (e.g. "python[0-9]?")
     * @param language target SupportedLanguage
     */
    public ShebangPattern(String regex, SupportedLanguage language) {
        Objects.requireNonNull(regex, "Regex must not be null");
        this.pattern = Pattern.compile(regex);
        this.language = Objects.requireNonNull(language, "Language must not be null");
    }

    /**
     * Checks if the shebang line matches the registered pattern.
     *
     * @param shebangLine the raw first line of a file
     * @return true if matches, false otherwise
     */
    public boolean matches(String shebangLine) {
        if (shebangLine == null) {
            return false;
        }
        return pattern.matcher(shebangLine).find();
    }

    public SupportedLanguage language() {
        return language;
    }
}
