package com.contextengine.infrastructure.parser;

import com.contextengine.domain.valueobject.Path;

/**
 * Interface contract for resolving specialized parsing engines matching target file extensions.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Dependency Engine (DN-SUB)
 * </p>
 */
public interface ILanguageParserFactory {

    /**
     * Resolves the appropriate ILanguageSymbolParser for the given file path.
     *
     * @param filePath path of the target file
     * @return language-specific parsing engine
     */
    ILanguageSymbolParser getParser(Path filePath);
}
