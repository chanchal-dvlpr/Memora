package com.contextengine.infrastructure.parser;

import com.contextengine.domain.valueobject.Path;
import java.util.Objects;

/**
 * Factory class maintaining a registry of parsers and returning matching parser instances based on file extensions.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Dependency Engine (DN-SUB)
 * </p>
 */
public class LanguageParserFactory implements ILanguageParserFactory {

    private final ILanguageSymbolParser tsParserBridge;
    private final ILanguageSymbolParser genericTextParser;

    /**
     * Constructs a LanguageParserFactory.
     */
    public LanguageParserFactory() {
        this.tsParserBridge = new TSParserBridge();
        this.genericTextParser = new GenericTextParser();
    }

    @Override
    public ILanguageSymbolParser getParser(Path filePath) {
        Objects.requireNonNull(filePath, "filePath must not be null");
        String pathVal = filePath.value().toLowerCase();
        if (pathVal.endsWith(".java") || pathVal.endsWith(".py") || pathVal.endsWith(".js") || 
            pathVal.endsWith(".ts") || pathVal.endsWith(".cpp") || pathVal.endsWith(".go")) {
            return tsParserBridge;
        }
        return genericTextParser;
    }
}
