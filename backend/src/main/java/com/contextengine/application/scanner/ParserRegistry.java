package com.contextengine.application.scanner;

import com.contextengine.domain.valueobject.Path;
import com.contextengine.infrastructure.parser.ILanguageParserFactory;
import com.contextengine.infrastructure.parser.ILanguageSymbolParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Concrete implementation of ParserRegistryBroker.
 * Coordinates extensions mapping to registered parsing engines and defaults.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * Dependencies: ILanguageParserFactory, ParserConfiguration, ParserCapabilityMatrix, Result
 * </p>
 */
public class ParserRegistry implements ParserRegistryBroker {

    private final ILanguageParserFactory languageParserFactory;
    private final List<ParserConfiguration> registeredParsers;

    /**
     * Constructs a ParserRegistry.
     *
     * @param languageParserFactory factory for language parsing engines
     */
    public ParserRegistry(ILanguageParserFactory languageParserFactory) {
        this.languageParserFactory = Objects.requireNonNull(languageParserFactory, "ILanguageParserFactory must not be null");
        this.registeredParsers = new ArrayList<>();

        // Pre-register default engines resolved by the factory
        List<String> defaultExtensions = List.of("java", "py", "js", "ts", "cpp", "go");
        for (String ext : defaultExtensions) {
            try {
                ILanguageSymbolParser parser = languageParserFactory.getParser(new Path("dummy." + ext));
                if (parser != null) {
                    boolean alreadyRegistered = false;
                    for (ParserConfiguration config : registeredParsers) {
                        if (config.parserInstance() == parser) {
                            if (!config.targetExtensions().contains(ext)) {
                                config.targetExtensions().add(ext);
                            }
                            alreadyRegistered = true;
                            break;
                        }
                    }
                    if (!alreadyRegistered) {
                        ParserConfiguration config = new ParserConfiguration(
                            parser.getClass().getSimpleName(),
                            new ArrayList<>(List.of(ext)),
                            new ParserCapabilityMatrix(Map.of("ast", true)),
                            parser
                        );
                        registeredParsers.add(config);
                    }
                }
            } catch (Exception e) {
                // Ignore resolution issues during startup
            }
        }
    }

    @Override
    public Result<ParserConfiguration, UnsupportedExtensionException> resolveParserForFile(String fileExtension) {
        Objects.requireNonNull(fileExtension, "FileExtension must not be null");
        String extension = fileExtension.toLowerCase().replace(".", "");

        // First look in explicitly registered engines
        for (ParserConfiguration config : registeredParsers) {
            if (config.targetExtensions().contains(extension)) {
                return Result.success(config);
            }
        }

        // Fallback to factory resolution
        try {
            ILanguageSymbolParser parser = languageParserFactory.getParser(new Path("dummy." + extension));
            if (parser != null) {
                ParserConfiguration config = new ParserConfiguration(
                    parser.getClass().getSimpleName(),
                    List.of(extension),
                    new ParserCapabilityMatrix(Map.of("ast", true)),
                    parser
                );
                return Result.success(config);
            }
        } catch (Exception e) {
            // Ignore and fall through to failure
        }

        return Result.failure(new UnsupportedExtensionException("No parser registered for extension: " + fileExtension));
    }

    @Override
    public Result<Void, RegistrationConflictException> registerParserEngine(
        String parserName,
        List<String> targetExtensions,
        ParserCapabilityMatrix capabilities
    ) {
        Objects.requireNonNull(parserName, "ParserName must not be null");
        Objects.requireNonNull(targetExtensions, "TargetExtensions must not be null");
        Objects.requireNonNull(capabilities, "Capabilities must not be null");

        // Check for conflicts
        for (String ext : targetExtensions) {
            String normExt = ext.toLowerCase().replace(".", "");
            for (ParserConfiguration config : registeredParsers) {
                if (config.targetExtensions().contains(normExt)) {
                    return Result.failure(new RegistrationConflictException(
                        "Conflict detected: Extension '" + normExt + "' is already registered to " + config.parserName()));
                }
            }
        }

        // Resolve parser instance using first extension
        ILanguageSymbolParser parserInstance = languageParserFactory.getParser(new Path("dummy." + targetExtensions.get(0)));

        ParserConfiguration newConfig = new ParserConfiguration(
            parserName,
            new ArrayList<>(targetExtensions),
            capabilities,
            parserInstance
        );
        registeredParsers.add(newConfig);

        return Result.success(null);
    }
}
