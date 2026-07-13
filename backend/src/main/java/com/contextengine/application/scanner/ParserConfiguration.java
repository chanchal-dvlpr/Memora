package com.contextengine.application.scanner;

import com.contextengine.infrastructure.parser.ILanguageSymbolParser;
import java.util.List;
import java.util.Objects;

/**
 * Configuration detailing a registered parser engine and its capabilities.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * Dependencies: ParserCapabilityMatrix, ILanguageSymbolParser
 * </p>
 */
public record ParserConfiguration(
    String parserName,
    List<String> targetExtensions,
    ParserCapabilityMatrix capabilities,
    ILanguageSymbolParser parserInstance
) {
    /**
     * Constructs a ParserConfiguration.
     *
     * @param parserName the descriptive name of the parser
     * @param targetExtensions target file extensions matched by this parser
     * @param capabilities capabilities matrix
     * @param parserInstance parser engine instance
     */
    public ParserConfiguration {
        Objects.requireNonNull(parserName, "ParserName must not be null");
        Objects.requireNonNull(targetExtensions, "TargetExtensions must not be null");
        Objects.requireNonNull(capabilities, "Capabilities must not be null");
        Objects.requireNonNull(parserInstance, "ParserInstance must not be null");
    }
}
