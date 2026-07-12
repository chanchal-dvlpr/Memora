package com.contextengine.application.usecase;

import com.contextengine.application.dto.ContextSnapshotDto;
import com.contextengine.application.exception.ApplicationException;
import com.contextengine.application.mapper.ContextSnapshotMapper;
import com.contextengine.application.query.GetLatestSnapshotQuery;
import com.contextengine.application.result.ApplicationResult;
import com.contextengine.domain.entity.ContextSnapshot;
import com.contextengine.domain.repository.ContextRepository;
import java.util.Objects;

/**
 * Use case coordinating retrieval of the latest compiled context snapshot for a project.
 * <p>
 * Bounded Context: Context Assembly
 * Related Query: GetLatestSnapshotQuery
 * Related Bounded Context / Aggregate: Context / ContextSnapshot
 * </p>
 */
public class GetLatestSnapshotUseCase implements UseCase<GetLatestSnapshotQuery, ApplicationResult<ContextSnapshotDto>> {

    private final ContextRepository contextRepository;

    /**
     * Constructs a GetLatestSnapshotUseCase.
     *
     * @param contextRepository repository interface
     */
    public GetLatestSnapshotUseCase(ContextRepository contextRepository) {
        this.contextRepository = Objects.requireNonNull(contextRepository, "ContextRepository must not be null");
    }

    @Override
    public ApplicationResult<ContextSnapshotDto> execute(GetLatestSnapshotQuery query) {
        try {
            Objects.requireNonNull(query, "Query must not be null");

            ContextSnapshot latest = contextRepository.findLatestSnapshotForProject(query.projectId())
                .orElseThrow(() -> new ApplicationException("No snapshots available for project: " + query.projectId().value()));

            return ApplicationResult.success(ContextSnapshotMapper.toDto(latest));
        } catch (Exception e) {
            return ApplicationResult.failure(new ApplicationException("Retrieval of latest snapshot failed", e));
        }
    }
}
