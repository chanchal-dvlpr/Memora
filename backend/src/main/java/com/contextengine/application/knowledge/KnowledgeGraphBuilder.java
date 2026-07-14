package com.contextengine.application.knowledge;

import com.contextengine.domain.entity.KnowledgeGraph;
import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.valueobject.Metadata;
import com.contextengine.domain.valueobject.NodeId;
import com.contextengine.application.scanner.ScanCandidate;
import com.contextengine.application.scanner.SourceSymbol;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Transforms scan candidates and source symbols into KnowledgeNode vertices, enforcing URN uniqueness.
 * <p>
 * Bounded Context: Knowledge Graph
 * Related Subsystem: Knowledge Graph Engine (KG-ENG)
 * Reference: Functional Requirement FR-016 (Knowledge Graph Engine) Section 7
 * </p>
 */
public class KnowledgeGraphBuilder {

    /**
     * Constructs a KnowledgeGraphBuilder.
     */
    public KnowledgeGraphBuilder() {
    }

    /**
     * Assembles nodes in the provided KnowledgeGraph.
     *
     * @param graph the knowledge graph aggregate root
     * @param candidates files discovered in the workspace
     * @param symbols source symbols extracted from files
     * @param context the construction session context
     */
    public void buildNodes(
        KnowledgeGraph graph,
        Collection<ScanCandidate> candidates,
        Collection<SourceSymbol> symbols,
        KnowledgeContext context
    ) {
        Objects.requireNonNull(graph, "KnowledgeGraph must not be null");
        Objects.requireNonNull(candidates, "Candidates must not be null");
        Objects.requireNonNull(symbols, "Symbols must not be null");
        Objects.requireNonNull(context, "KnowledgeContext must not be null");

        String projectIdStr = graph.projectId().value().toString();

        java.util.Set<NodeId> existingNodeIds = new java.util.HashSet<>();
        for (KnowledgeNode n : graph.nodes()) {
            existingNodeIds.add(n.id());
        }

        // 1. Create Project Node
        String projUrn = "urn:ce:node:" + projectIdStr + ":project:" + projectIdStr;
        NodeId projNodeId = context.resolveNodeId(projUrn);
        Map<String, String> projAttrs = new HashMap<>();
        projAttrs.put("urn", projUrn);
        projAttrs.put("name", projectIdStr);
        projAttrs.put("projectId", projectIdStr);
        projAttrs.put("qualifiedName", projectIdStr);
        if (!existingNodeIds.contains(projNodeId)) {
            graph.addNode(new KnowledgeNode(projNodeId, "PROJECT", new Metadata(projAttrs)));
            existingNodeIds.add(projNodeId);
        }

        // 2. Create File Nodes from Candidates
        for (ScanCandidate candidate : candidates) {
            String cleanRelPath = candidate.relativePath().replace("\\", "/");
            String fileHash = generateHash(cleanRelPath);
            String fileUrn = "urn:ce:node:" + projectIdStr + ":file:" + fileHash.substring(0, 16);
            NodeId fileNodeId = context.resolveNodeId(fileUrn);

            Map<String, String> fileAttrs = new HashMap<>();
            fileAttrs.put("urn", fileUrn);
            fileAttrs.put("name", cleanRelPath);
            fileAttrs.put("projectId", projectIdStr);
            fileAttrs.put("qualifiedName", cleanRelPath);
            fileAttrs.put("filePath", candidate.absolutePath());
            fileAttrs.put("size", String.valueOf(candidate.size()));
            fileAttrs.put("language", candidate.language().name());

            if (!existingNodeIds.contains(fileNodeId)) {
                graph.addNode(new KnowledgeNode(fileNodeId, "FILE", new Metadata(fileAttrs)));
                existingNodeIds.add(fileNodeId);
            }
        }

        // 3. Create Symbol Nodes from SourceSymbols
        for (SourceSymbol symbol : symbols) {
            String cleanKind = mapKindToCategory(symbol.kind());
            String cleanFilePath = symbol.filePath().replace("\\", "/");
            String signature = symbol.name() + ":" + cleanFilePath;
            String symHash = generateHash(signature);
            String symUrn = "urn:ce:node:" + projectIdStr + ":" + cleanKind + ":" + symHash.substring(0, 16);

            NodeId symNodeId = context.resolveNodeId(symUrn);

            Map<String, String> symAttrs = new HashMap<>();
            symAttrs.put("urn", symUrn);
            symAttrs.put("name", symbol.name());
            symAttrs.put("projectId", projectIdStr);
            symAttrs.put("qualifiedName", symbol.name());
            symAttrs.put("kind", symbol.kind());
            symAttrs.put("filePath", cleanFilePath);
            symAttrs.put("sourceRange", symbol.startLine() + "-" + symbol.endLine());
            symAttrs.putAll(symbol.metadata());

            if (!existingNodeIds.contains(symNodeId)) {
                graph.addNode(new KnowledgeNode(symNodeId, symbol.kind().toUpperCase(), new Metadata(symAttrs)));
                existingNodeIds.add(symNodeId);
            }
        }
    }

    private String mapKindToCategory(String kind) {
        if (kind == null) return "unknown";
        String lower = kind.trim().toLowerCase();
        switch (lower) {
            case "class":
                return "class";
            case "interface":
                return "interface";
            case "method":
            case "function":
                return "function";
            case "file":
                return "file";
            default:
                return lower.replaceAll("[^a-z0-9_]", "_");
        }
    }

    private String generateHash(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 algorithm failure", e);
        }
    }
}
