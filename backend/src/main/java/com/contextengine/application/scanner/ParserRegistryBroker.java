package com.contextengine.application.scanner;

import java.util.List;

/**
 * Manages syntax parsers, matching file extensions to appropriate parsing engines.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * Dependencies: Result, ParserConfiguration, UnsupportedExtensionException, RegistrationConflictException, ParserCapabilityMatrix
 * </p>
 */
public interface ParserRegistryBroker {

    /**
     * Resolves the appropriate parser for a given file extension or MIME type.
     *
     * @param fileExtension target file extension (e.g., "java")
     * @return Result containing either the ParserConfiguration or an UnsupportedExtensionException
     */
    Result<ParserConfiguration, UnsupportedExtensionException> resolveParserForFile(String fileExtension);

    /**
     * Registers a new parsing engine with the workspace.
     *
     * @param parserName unique parser name
     * @param targetExtensions list of file extensions supported by the parser
     * @param capabilities capabilities matrix
     * @return Result containing either Void or a RegistrationConflictException
     */
    Result<Void, RegistrationConflictException> registerParserEngine(
        String parserName,
        List<String> targetExtensions,
        ParserCapabilityMatrix capabilities
    );
}
