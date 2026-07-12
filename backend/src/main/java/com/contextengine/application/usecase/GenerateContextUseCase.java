package com.contextengine.application.usecase;

import com.contextengine.application.command.GenerateContextCommand;
import com.contextengine.application.dto.ContextSnapshotDto;
import com.contextengine.application.exception.ApplicationException;
import com.contextengine.application.mapper.ContextSnapshotMapper;
import com.contextengine.application.result.ApplicationResult;
import com.contextengine.domain.entity.Context;
import com.contextengine.domain.entity.ContextSnapshot;
import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.repository.ContextRepository;
import com.contextengine.domain.repository.KnowledgeGraphRepository;
import com.contextengine.domain.service.ContextGenerationService;
import com.contextengine.domain.valueobject.Version;
import java.util.Collection;
import java.util.Objects;

/**
 * Use case coordinating the assembly, token budget enforcement, and serialization of context snapshots.
 * <p>
 * Bounded Context: Context Assembly
 * Related Command: GenerateContextCommand
 * Related Bounded Context / Aggregate: Context / ContextSnapshot
 * </p>
 */
public class GenerateContextUseCase implements UseCase<GenerateContextCommand, ApplicationResult<ContextSnapshotDto>> {

    private final ContextRepository contextRepository;
    private final KnowledgeGraphRepository graphRepository;
    private final ContextGenerationService generationService;

    /**
     * Constructs a GenerateContextUseCase.
     *
     * @param contextRepository repository interface
     * @param graphRepository knowledge graph repository interface
     * @param generationService domain context generation service
     */
    public GenerateContextUseCase(
        ContextRepository contextRepository,
        KnowledgeGraphRepository graphRepository,
        ContextGenerationService generationService
    ) {
        this.contextRepository = Objects.requireNonNull(contextRepository, "ContextRepository must not be null");
        this.graphRepository = Objects.requireNonNull(graphRepository, "KnowledgeGraphRepository must not be null");
        this.generationService = Objects.requireNonNull(generationService, "ContextGenerationService must not be null");
    }

    @Override
    public ApplicationResult<ContextSnapshotDto> execute(GenerateContextCommand command) {
        try {
            Objects.requireNonNull(command, "Command must not be null");

            Collection<KnowledgeNode> nodes = graphRepository.querySubGraph(command.focusNode(), 3);

            int nextVersion = 1;
            java.util.Optional<ContextSnapshot> latestSnapshot = contextRepository.findLatestSnapshotForProject(command.projectId());
            if (latestSnapshot.isPresent()) {
                nextVersion = latestSnapshot.get().version().value() + 1;
            }

            ContextSnapshot snapshot = generationService.generateSnapshot(
                command.projectId(),
                new Version(nextVersion),
                nodes,
                command.budget(),
                command.format()
            );

            Context context = new Context(command.projectId(), command.budget());
            context.addSnapshot(snapshot);
            contextRepository.save(context);

            return ApplicationResult.success(ContextSnapshotMapper.toDto(snapshot));
        } catch (Exception e) {
            return ApplicationResult.failure(new ApplicationException("Context generation failed", e));
        }
    }
}
