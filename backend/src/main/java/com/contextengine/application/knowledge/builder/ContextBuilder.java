package com.contextengine.application.knowledge.builder;

import com.contextengine.application.knowledge.ranking.ContextRankedResult;
import com.contextengine.domain.entity.ContextSnapshot;
import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.valueobject.ContextSummary;
import com.contextengine.domain.valueobject.EngineeringEvidence;
import com.contextengine.domain.valueobject.Hash;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.SnapshotId;
import com.contextengine.domain.valueobject.Timestamp;
import com.contextengine.domain.valueobject.Version;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Assembles ranked knowledge entities and relationships into immutable, compressed Context Snapshots.
 * <p>
 * Bounded Context: Knowledge Graph / Context Assembly
 * Architecture Reference: Functional Requirement FR-014 (Context Generation Engine) Section 8 (Lifecycle state: ASSEMBLED)
 * Responsibility: Packs ranked candidates into ContextSnapshot envelopes preserving provenance maps and engineering evidence.
 * Dependencies: {@link ContextSnapshot}, {@link ContextRankedResult}, {@link ProjectId}, {@link Version}.
 * Future Usage: Integration with token budget management pipelines and custom views.
 * </p>
 */
public class ContextBuilder {

    private static final String DEFAULT_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    /**
     * Constructs a ContextBuilder.
     */
    public ContextBuilder() {
        // Default constructor
    }

    /**
     * Compiles a collection of ranked nodes into an immutable ContextSnapshot.
     *
     * @param projectId the associated project ID
     * @param version the sequence version for the snapshot
     * @param rankedResults sorted relevance ranking results
     * @return the assembled ContextSnapshot
     */
    public ContextSnapshot build(
        ProjectId projectId,
        Version version,
        List<ContextRankedResult> rankedResults
    ) {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(version, "Version must not be null");
        Objects.requireNonNull(rankedResults, "RankedResults must not be null");

        int totalFileCount = 0;
        int tokenFootprint = 0;
        List<String> primaryEntities = new ArrayList<>();
        List<EngineeringEvidence> evidences = new ArrayList<>();

        for (ContextRankedResult ranked : rankedResults) {
            KnowledgeNode node = ranked.node();

            // Track file count
            if (node.type().equalsIgnoreCase("FILE")) {
                totalFileCount++;
            }

            // Accumulate token footprint using same calculation standard as ContextGenerationService
            int nodeTokens = 10;
            String tokensAttr = node.attributes().get("tokens");
            if (tokensAttr != null) {
                try {
                    nodeTokens = Integer.parseInt(tokensAttr);
                } catch (NumberFormatException ignored) {}
            }
            tokenFootprint += nodeTokens;

            // Preserve citation reference (URN)
            String urn = node.attributes().get("urn");
            if (urn == null) {
                urn = "urn:ce:node:" + projectId.value().toString() + ":" + node.type().toLowerCase() + ":" + node.id().value().toString();
            }
            primaryEntities.add(urn);

            // Extract physical evidence coordinates if available
            String filePathStr = node.attributes().get("filePath");
            if (filePathStr == null && node.type().equalsIgnoreCase("FILE")) {
                filePathStr = node.attributes().get("qualifiedName");
                if (filePathStr == null) {
                    filePathStr = node.attributes().get("name");
                }
            }

            if (filePathStr != null && !filePathStr.isBlank()) {
                int startLine = 1;
                int endLine = 1;

                String startLineAttr = node.attributes().get("startLine");
                String endLineAttr = node.attributes().get("endLine");
                if (startLineAttr != null && endLineAttr != null) {
                    try {
                        startLine = Integer.parseInt(startLineAttr);
                        endLine = Integer.parseInt(endLineAttr);
                        if (startLine < 1) {
                            startLine = 1;
                        }
                        if (endLine < startLine) {
                            endLine = startLine;
                        }
                    } catch (NumberFormatException ignored) {}
                }

                Hash contentHash = resolveHash(node);
                try {
                    EngineeringEvidence evidence = new EngineeringEvidence(
                        new Path(filePathStr),
                        startLine,
                        endLine,
                        contentHash
                    );
                    evidences.add(evidence);
                } catch (IllegalArgumentException ignored) {
                    // Ignore elements with invalid path characters or boundaries
                }
            }
        }

        ContextSummary summary = new ContextSummary(totalFileCount, tokenFootprint, primaryEntities);
        return new ContextSnapshot(
            SnapshotId.generate(),
            projectId,
            version,
            Timestamp.now(),
            summary,
            evidences
        );
    }

    private Hash resolveHash(KnowledgeNode node) {
        String hashStr = node.attributes().get("fileContentHash");
        if (hashStr == null && node.type().equalsIgnoreCase("FILE")) {
            String urn = node.attributes().get("urn");
            if (urn != null) {
                String[] parts = urn.split(":");
                String lastPart = parts[parts.length - 1];
                if (lastPart.matches("^[0-9a-fA-F]{64}$")) {
                    hashStr = lastPart;
                }
            }
        }
        if (hashStr == null) {
            hashStr = DEFAULT_HASH;
        }
        return new Hash(hashStr);
    }
}
