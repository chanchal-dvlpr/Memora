package com.contextengine.infrastructure.parser;

import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.valueobject.Metadata;
import com.contextengine.domain.valueobject.NodeId;
import com.contextengine.domain.valueobject.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Fallback plain-text parser mapping unsupported files to generic text symbols.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Dependency Engine (DN-SUB)
 * </p>
 */
public class GenericTextParser implements ILanguageSymbolParser {

    @Override
    public Collection<KnowledgeNode> parse(Path filePath, String fileContent) {
        String name = java.nio.file.Paths.get(filePath.value()).getFileName().toString();
        Metadata attributes = new Metadata(Map.of(
            "name", name,
            "path", filePath.value(),
            "kind", "TEXT_FILE",
            "size", String.valueOf(fileContent.length())
        ));
        KnowledgeNode node = new KnowledgeNode(NodeId.generate(), "CODE_SYMBOL", attributes);
        return List.of(node);
    }
}
