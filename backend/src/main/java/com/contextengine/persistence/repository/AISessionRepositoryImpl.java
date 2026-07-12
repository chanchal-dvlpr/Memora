package com.contextengine.persistence.repository;

import com.contextengine.domain.entity.AISession;
import com.contextengine.domain.repository.AISessionRepository;
import com.contextengine.domain.valueobject.SessionId;
import com.contextengine.persistence.mapper.AISessionPersistenceMapper;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Technical persistence implementation of the AISessionRepository using Spring Data JPA.
 * <p>
 * Bounded Context: AI Ingestion
 * Related Subsystem: Persistent Storage
 * </p>
 */
public class AISessionRepositoryImpl implements AISessionRepository {

    private final SpringDataAISessionRepository springDataRepository;
    private final AISessionPersistenceMapper mapper = new AISessionPersistenceMapper();

    /**
     * Constructs an AISessionRepositoryImpl.
     *
     * @param springDataRepository the spring data JPA repository
     */
    public AISessionRepositoryImpl(SpringDataAISessionRepository springDataRepository) {
        this.springDataRepository = Objects.requireNonNull(springDataRepository, "SpringDataAISessionRepository must not be null");
    }

    @Override
    public void save(AISession session) {
        Objects.requireNonNull(session, "AISession must not be null");
        springDataRepository.save(mapper.toEntity(session));
    }

    @Override
    public Optional<AISession> findById(SessionId sessionId) {
        Objects.requireNonNull(sessionId, "SessionId must not be null");
        return springDataRepository.findById(sessionId.value().toString())
            .map(mapper::toDomain);
    }

    @Override
    public Collection<AISession> findByClientIdentity(String clientApp) {
        Objects.requireNonNull(clientApp, "ClientApp must not be null");
        return springDataRepository.findByClientIdentity(clientApp).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public void remove(SessionId sessionId) {
        Objects.requireNonNull(sessionId, "SessionId must not be null");
        springDataRepository.deleteById(sessionId.value().toString());
    }
}
