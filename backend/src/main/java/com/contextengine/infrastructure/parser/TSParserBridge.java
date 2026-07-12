package com.contextengine.infrastructure.parser;

import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.valueobject.Metadata;
import com.contextengine.domain.valueobject.NodeId;
import com.contextengine.domain.valueobject.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Tree-sitter interface boundary simulating native AST symbol extraction.
 * Parses classes, interfaces, and methods.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Dependency Engine (DN-SUB)
 * </p>
 */
public class TSParserBridge implements ILanguageSymbolParser {

    @Override
    public Collection<KnowledgeNode> parse(Path filePath, String fileContent) {
        List<KnowledgeNode> symbols = new ArrayList<>();
        
        String[] lines = fileContent.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("class ") || line.startsWith("public class ") || line.startsWith("interface ")) {
                String name = extractName(line, "class ");
                if (name == null) {
                    name = extractName(line, "interface ");
                }
                if (name != null) {
                    Metadata attributes = new Metadata(Map.of(
                        "name", name,
                        "path", filePath.value(),
                        "kind", "CLASS",
                        "line", String.valueOf(i + 1)
                    ));
                    symbols.add(new KnowledgeNode(NodeId.generate(), "CODE_SYMBOL", attributes));
                }
            } else if (line.contains("void ") || line.contains("public ") && line.contains("(")) {
                String name = extractMethodName(line);
                if (name != null && !name.equals("class") && !name.equals("if") && !name.equals("for") && !name.equals("while")) {
                    Metadata attributes = new Metadata(Map.of(
                        "name", name,
                        "path", filePath.value(),
                        "kind", "METHOD",
                        "line", String.valueOf(i + 1)
                    ));
                    symbols.add(new KnowledgeNode(NodeId.generate(), "CODE_SYMBOL", attributes));
                }
            }
        }

        if (symbols.isEmpty()) {
            String name = java.nio.file.Paths.get(filePath.value()).getFileName().toString();
            Metadata attributes = new Metadata(Map.of(
                "name", name,
                "path", filePath.value(),
                "kind", "FILE",
                "size", String.valueOf(fileContent.length())
            ));
            symbols.add(new KnowledgeNode(NodeId.generate(), "CODE_SYMBOL", attributes));
        }

        return symbols;
    }

    private String extractName(String line, String keyword) {
        int idx = line.indexOf(keyword);
        if (idx == -1) {
            return null;
        }
        String sub = line.substring(idx + keyword.length()).trim();
        int spaceIdx = sub.indexOf(" ");
        int braceIdx = sub.indexOf("{");
        int end = sub.length();
        if (spaceIdx != -1) {
            end = spaceIdx;
        }
        if (braceIdx != -1 && braceIdx < end) {
            end = braceIdx;
        }
        return sub.substring(0, end).trim();
    }

    private String extractMethodName(String line) {
        int parenIdx = line.indexOf("(");
        if (parenIdx == -1) {
            return null;
        }
        String left = line.substring(0, parenIdx).trim();
        int lastSpace = left.lastIndexOf(" ");
        if (lastSpace == -1) {
            return left;
        }
        return left.substring(lastSpace + 1).trim();
    }
}
