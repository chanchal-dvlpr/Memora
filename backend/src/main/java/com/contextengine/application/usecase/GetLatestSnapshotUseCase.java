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

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GetLatestSnapshotUseCase.class);
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
                .orElseThrow(() -> new com.contextengine.application.exception.SnapshotNotFoundException("No snapshots available for project: " + query.projectId().value()));

            return ApplicationResult.success(ContextSnapshotMapper.toDto(latest));
        } catch (com.contextengine.application.exception.SnapshotNotFoundException e) {
            logger.warn("No snapshots found for project {}: {}", query.projectId().value(), e.getMessage());
            return ApplicationResult.failure(e);
        } catch (Exception e) {
            logger.error("Failed to retrieve latest snapshot for project {}", query.projectId().value(), e);
            return ApplicationResult.failure(e instanceof ApplicationException ? (ApplicationException) e : new ApplicationException(e.getMessage(), e));
        }
    }
}
