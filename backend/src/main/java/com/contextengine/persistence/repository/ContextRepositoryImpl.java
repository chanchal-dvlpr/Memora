package com.contextengine.persistence.repository;

import com.contextengine.domain.entity.Context;
import com.contextengine.domain.entity.ContextSnapshot;
import com.contextengine.domain.repository.ContextRepository;
import com.contextengine.domain.valueobject.DateRange;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.SnapshotId;
import com.contextengine.domain.valueobject.Timestamp;
import com.contextengine.persistence.mapper.ContextSnapshotPersistenceMapper;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Technical persistence implementation of the ContextRepository using Spring Data JPA.
 * <p>
 * Bounded Context: Context Assembly
 * Related Subsystem: Persistent Storage
 * </p>
 */
public class ContextRepositoryImpl implements ContextRepository {

    private final SpringDataContextSnapshotRepository springDataRepository;
    private final ContextSnapshotPersistenceMapper mapper = new ContextSnapshotPersistenceMapper();

    /**
     * Constructs a ContextRepositoryImpl.
     *
     * @param springDataRepository the spring data JPA repository
     */
    public ContextRepositoryImpl(SpringDataContextSnapshotRepository springDataRepository) {
        this.springDataRepository = Objects.requireNonNull(springDataRepository, "SpringDataContextSnapshotRepository must not be null");
    }

    @Override
    @Transactional
    public void save(Context context) {
        Objects.requireNonNull(context, "Context must not be null");
        for (ContextSnapshot snapshot : context.snapshots()) {
            springDataRepository.save(mapper.toEntity(snapshot));
        }
    }

    @Override
    public Optional<ContextSnapshot> findSnapshotById(SnapshotId snapshotId) {
        Objects.requireNonNull(snapshotId, "SnapshotId must not be null");
        return springDataRepository.findById(snapshotId.value().toString())
            .map(mapper::toDomain);
    }

    @Override
    public Optional<ContextSnapshot> findLatestSnapshotForProject(ProjectId projectId) {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        return springDataRepository.findLatestForProject(projectId.value().toString())
            .map(mapper::toDomain);
    }

    @Override
    public Collection<ContextSnapshot> findHistoryByDateRange(ProjectId projectId, DateRange range) {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(range, "DateRange must not be null");
        return springDataRepository.findHistoryByDateRange(
            projectId.value().toString(),
            range.start().value(),
            range.end().value()
        ).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public int pruneOldSnapshots(ProjectId projectId, Timestamp retentionCutoff) {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(retentionCutoff, "RetentionCutoff must not be null");
        return springDataRepository.pruneOldSnapshots(projectId.value().toString(), retentionCutoff.value());
    }

    @Override
    @Transactional
    public void removeSnapshot(SnapshotId snapshotId) {
        Objects.requireNonNull(snapshotId, "SnapshotId must not be null");
        springDataRepository.deleteById(snapshotId.value().toString());
    }
}
