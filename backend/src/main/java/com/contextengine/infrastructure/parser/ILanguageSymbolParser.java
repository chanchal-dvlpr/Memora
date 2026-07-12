package com.contextengine.infrastructure.parser;

import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.valueobject.Path;
import java.util.Collection;

/**
 * Interface contract for language-specific AST symbol parsing engines.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Dependency Engine (DN-SUB)
 * </p>
 */
public interface ILanguageSymbolParser {

    /**
     * Parses the file content to extract declarations, properties, and structural symbols.
     *
     * @param filePath path of the parsed file
     * @param fileContent raw content of the file
     * @return collection of extracted symbol nodes
     */
    Collection<KnowledgeNode> parse(Path filePath, String fileContent);
}
