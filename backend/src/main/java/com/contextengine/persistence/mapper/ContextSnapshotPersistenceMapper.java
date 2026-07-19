package com.contextengine.persistence.mapper;

import com.contextengine.domain.entity.ContextSnapshot;
import com.contextengine.domain.valueobject.ContextSummary;
import com.contextengine.domain.valueobject.EngineeringEvidence;
import com.contextengine.domain.valueobject.Hash;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.SnapshotId;
import com.contextengine.domain.valueobject.Timestamp;
import com.contextengine.domain.valueobject.Version;
import com.contextengine.persistence.entity.ContextSnapshotEntity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Persistence mapper converting between ContextSnapshot domain model and ContextSnapshotEntity.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Persistent Storage
 * </p>
 */
public class ContextSnapshotPersistenceMapper {

    /**
     * Converts ContextSnapshot domain model to ContextSnapshotEntity.
     *
     * @param snapshot domain model
     * @return snapshot entity
     */
    public ContextSnapshotEntity toEntity(ContextSnapshot snapshot) {
        if (snapshot == null) return null;
        ContextSnapshotEntity entity = new ContextSnapshotEntity();
        entity.setId(snapshot.id().value().toString());
        entity.setProjectId(snapshot.projectId().value().toString());
        entity.setVersionNumber(snapshot.version().value());
        entity.setSnapshotCreatedAt(snapshot.createdAt().value());
        entity.setTotalFileCount(snapshot.summary().totalFileCount());
        entity.setTokenFootprint(snapshot.summary().tokenFootprint());

        String entities = String.join(",", snapshot.summary().primaryEntities());
        entity.setPrimaryEntitiesSerialized(entities);

        String evidences = snapshot.evidences().stream()
            .map(e -> e.filePath().value() + ":" + e.startLine() + ":" + e.endLine() + ":" + e.fileContentHash().value())
            .collect(Collectors.joining("|"));
        entity.setEvidencesSerialized(evidences);
        entity.setPayload(snapshot.payload());

        return entity;
    }

    /**
     * Converts ContextSnapshotEntity to ContextSnapshot domain model.
     *
     * @param entity snapshot entity
     * @return snapshot domain model
     */
    public ContextSnapshot toDomain(ContextSnapshotEntity entity) {
        if (entity == null) return null;
        SnapshotId id = new SnapshotId(UUID.fromString(entity.getId()));
        ProjectId projectId = new ProjectId(UUID.fromString(entity.getProjectId()));
        Version version = new Version(entity.getVersionNumber());
        Timestamp createdAt = new Timestamp(entity.getSnapshotCreatedAt());

        List<String> entities = entity.getPrimaryEntitiesSerialized().isEmpty()
            ? Collections.emptyList()
            : Arrays.asList(entity.getPrimaryEntitiesSerialized().split(","));

        ContextSummary summary = new ContextSummary(
            entity.getTotalFileCount(),
            entity.getTokenFootprint(),
            entities
        );

        List<EngineeringEvidence> evidences = new ArrayList<>();
        if (!entity.getEvidencesSerialized().isEmpty()) {
            String[] parts = entity.getEvidencesSerialized().split("\\|");
            for (String part : parts) {
                String[] fields = part.split(":");
                if (fields.length == 4) {
                    evidences.add(new EngineeringEvidence(
                        new Path(fields[0]),
                        Integer.parseInt(fields[1]),
                        Integer.parseInt(fields[2]),
                        new Hash(fields[3])
                    ));
                }
            }
        }

        return new ContextSnapshot(id, projectId, version, createdAt, summary, evidences, entity.getPayload());
    }
}
