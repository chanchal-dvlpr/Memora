package com.contextengine.application.usecase;

import com.contextengine.application.command.GenerateContextCommand;
import com.contextengine.application.dto.ContextSnapshotDto;
import com.contextengine.application.exception.ApplicationException;
import com.contextengine.application.mapper.ContextSnapshotMapper;
import com.contextengine.application.result.ApplicationResult;
import com.contextengine.application.knowledge.KnowledgeOptimizer;
import com.contextengine.application.knowledge.budget.TokenBudgetManager;
import com.contextengine.application.knowledge.builder.ContextBuilder;
import com.contextengine.application.knowledge.ranking.ContextRankedResult;
import com.contextengine.application.knowledge.ranking.RankingEngine;
import com.contextengine.application.knowledge.retrieval.RetrievalEngine;
import com.contextengine.application.knowledge.retrieval.RetrievalQuery;
import com.contextengine.application.knowledge.retrieval.RetrievalResult;
import com.contextengine.application.knowledge.validation.ContextValidator;
import com.contextengine.domain.entity.Context;
import com.contextengine.domain.entity.ContextSnapshot;
import com.contextengine.domain.event.ContextAssembled;
import com.contextengine.domain.event.DomainEventPublisher;
import com.contextengine.domain.repository.ContextRepository;
import com.contextengine.domain.valueobject.Metadata;
import com.contextengine.domain.valueobject.SnapshotId;
import com.contextengine.domain.valueobject.Version;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private final RetrievalEngine retrievalEngine;
    private final RankingEngine rankingEngine;
    private final ContextBuilder contextBuilder;
    private final TokenBudgetManager tokenBudgetManager;
    private final ContextValidator contextValidator;
    private final KnowledgeOptimizer knowledgeOptimizer;
    private final DomainEventPublisher eventPublisher;

    /**
     * Constructs a GenerateContextUseCase.
     *
     * @param contextRepository repository interface
     * @param retrievalEngine retrieval engine component
     * @param rankingEngine ranking engine component
     * @param contextBuilder context snapshot builder component
     * @param tokenBudgetManager token budget manager component
     * @param contextValidator context validator component
     * @param knowledgeOptimizer knowledge optimizer component
     * @param eventPublisher domain event publisher
     */
    public GenerateContextUseCase(
        ContextRepository contextRepository,
        RetrievalEngine retrievalEngine,
        RankingEngine rankingEngine,
        ContextBuilder contextBuilder,
        TokenBudgetManager tokenBudgetManager,
        ContextValidator contextValidator,
        KnowledgeOptimizer knowledgeOptimizer,
        DomainEventPublisher eventPublisher
    ) {
        this.contextRepository = Objects.requireNonNull(contextRepository, "ContextRepository must not be null");
        this.retrievalEngine = Objects.requireNonNull(retrievalEngine, "RetrievalEngine must not be null");
        this.rankingEngine = Objects.requireNonNull(rankingEngine, "RankingEngine must not be null");
        this.contextBuilder = Objects.requireNonNull(contextBuilder, "ContextBuilder must not be null");
        this.tokenBudgetManager = Objects.requireNonNull(tokenBudgetManager, "TokenBudgetManager must not be null");
        this.contextValidator = Objects.requireNonNull(contextValidator, "ContextValidator must not be null");
        this.knowledgeOptimizer = Objects.requireNonNull(knowledgeOptimizer, "KnowledgeOptimizer must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "DomainEventPublisher must not be null");
    }

    @Override
    public ApplicationResult<ContextSnapshotDto> execute(GenerateContextCommand command) {
        try {
            Objects.requireNonNull(command, "Command must not be null");

            String queryText = command.query().term();
            String activeFilePath = null;
            if (queryText.contains(".") || queryText.contains("/")) {
                activeFilePath = queryText;
            }

            // 1. Optimization & Retrieval
            RetrievalQuery retrievalQuery = new RetrievalQuery(
                command.projectId(),
                queryText,
                activeFilePath,
                3
            );
            RetrievalResult retrievalResult = knowledgeOptimizer.optimizeRetrieval(retrievalQuery, () ->
                retrievalEngine.retrieve(retrievalQuery)
            );

            // 2. Ranking
            List<ContextRankedResult> rankedResults = rankingEngine.rank(retrievalResult, queryText, activeFilePath);

            // 3. Token Budget
            SnapshotId contextId = SnapshotId.generate();
            List<ContextRankedResult> budgetedResults = tokenBudgetManager.applyBudget(
                rankedResults,
                command.budget(),
                command.projectId(),
                contextId
            );

            // 4. Context Snapshot Assembly
            int nextVersion = 1;
            java.util.Optional<ContextSnapshot> latestSnapshot = contextRepository.findLatestSnapshotForProject(command.projectId());
            if (latestSnapshot.isPresent()) {
                nextVersion = latestSnapshot.get().version().value() + 1;
            }

            ContextSnapshot snapshot = contextBuilder.build(
                command.projectId(),
                new Version(nextVersion),
                budgetedResults
            );
            // Re-bind to use the generated contextId to align with events
            snapshot = new ContextSnapshot(
                contextId,
                snapshot.projectId(),
                snapshot.version(),
                snapshot.createdAt(),
                snapshot.summary(),
                snapshot.evidences()
            );

            // 5. Validation
            contextValidator.validate(snapshot, command.budget());

            // 6. Persistence & Save
            Context context = new Context(command.projectId(), command.budget());
            context.addSnapshot(snapshot);
            contextRepository.save(context);

            // 7. Publish Assembled Event
            Instant now = Instant.now();
            List<String> focusFiles = activeFilePath != null ? List.of(activeFilePath) : List.of();
            int avgTokens = snapshot.summary().tokenFootprint() / Math.max(1, snapshot.evidences().size());
            List<Metadata> elements = snapshot.evidences().stream()
                .map(ev -> new Metadata(Map.of(
                    "element_type", "FILE",
                    "identifier", ev.filePath().value(),
                    "tokens", String.valueOf(avgTokens)
                )))
                .collect(Collectors.toList());

            ContextAssembled assembledEvent = new ContextAssembled(
                contextId,
                command.projectId(),
                focusFiles,
                snapshot.summary().tokenFootprint(),
                "CODE_ONLY",
                elements,
                now
            );
            eventPublisher.publish(assembledEvent);

            return ApplicationResult.success(ContextSnapshotMapper.toDto(snapshot));
        } catch (Exception e) {
            return ApplicationResult.failure(new ApplicationException("Context generation failed", e));
        }
    }
}
