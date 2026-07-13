package com.contextengine.application.scanner;

import com.contextengine.domain.entity.KnowledgeNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Service responsible for extracting and normalizing symbols from raw parser nodes.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * Dependencies: KnowledgeNode, SourceSymbol
 * </p>
 */
public class SymbolExtractor {

    /**
     * Constructs a SymbolExtractor.
     */
    public SymbolExtractor() {
        // Default constructor
    }

    /**
     * Extracts and normalizes raw parser nodes into a collection of SourceSymbol objects.
     *
     * @param nodes raw knowledge nodes returned by a parser
     * @return normalized collection of SourceSymbols
     */
    public Collection<SourceSymbol> extract(Collection<KnowledgeNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyList();
        }

        Collection<SourceSymbol> symbols = new ArrayList<>();
        for (KnowledgeNode node : nodes) {
            if (node == null || node.attributes() == null) {
                continue;
            }

            Map<String, String> values = node.attributes().values();
            String name = values.getOrDefault("name", "Unknown");
            String kind = values.getOrDefault("kind", "UNKNOWN");
            String filePath = values.getOrDefault("path", "unknown");

            int startLine = 1;
            String lineStr = values.get("line");
            if (lineStr != null) {
                try {
                    startLine = Integer.parseInt(lineStr);
                    if (startLine < 1) {
                        startLine = 1;
                    }
                } catch (NumberFormatException e) {
                    // Fallback to default
                }
            }

            int endLine = startLine;
            String endLineStr = values.get("end_line");
            if (endLineStr != null) {
                try {
                    int parsedEnd = Integer.parseInt(endLineStr);
                    if (parsedEnd >= startLine) {
                        endLine = parsedEnd;
                    }
                } catch (NumberFormatException e) {
                    // Fallback to startLine
                }
            }

            // Create metadata map with everything except the primary fields
            Map<String, String> extraMetadata = new HashMap<>(values);
            extraMetadata.remove("name");
            extraMetadata.remove("kind");
            extraMetadata.remove("path");
            extraMetadata.remove("line");
            extraMetadata.remove("end_line");

            SourceSymbol symbol = new SourceSymbol(
                name,
                kind,
                filePath,
                startLine,
                endLine,
                extraMetadata
            );
            symbols.add(symbol);
        }

        return symbols;
    }
}
