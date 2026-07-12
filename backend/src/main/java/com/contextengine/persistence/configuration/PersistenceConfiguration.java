package com.contextengine.persistence.configuration;

import com.contextengine.application.port.TransactionManager;
import com.contextengine.domain.repository.AISessionRepository;
import com.contextengine.domain.repository.ContextRepository;
import com.contextengine.domain.repository.KnowledgeGraphRepository;
import com.contextengine.domain.repository.ProjectRepository;
import com.contextengine.persistence.repository.AISessionRepositoryImpl;
import com.contextengine.persistence.repository.ContextRepositoryImpl;
import com.contextengine.persistence.repository.KnowledgeGraphRepositoryImpl;
import com.contextengine.persistence.repository.ProjectRepositoryImpl;
import com.contextengine.persistence.repository.SpringDataAISessionRepository;
import com.contextengine.persistence.repository.SpringDataContextSnapshotRepository;
import com.contextengine.persistence.repository.SpringDataKnowledgeNodeRepository;
import com.contextengine.persistence.repository.SpringDataKnowledgeRelationshipRepository;
import com.contextengine.persistence.repository.SpringDataProjectRepository;
import com.contextengine.persistence.transaction.SpringTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Spring configuration class registering database repositories as beans.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Persistent Storage
 * </p>
 */
@Configuration
public class PersistenceConfiguration {

    /**
     * Registers TransactionManager.
     *
     * @param transactionTemplate spring transaction template
     * @return TransactionManager bean
     */
    @Bean
    public TransactionManager applicationTransactionManager(TransactionTemplate transactionTemplate) {
        return new SpringTransactionManager(transactionTemplate);
    }

    /**
     * Registers ProjectRepository.
     *
     * @param springDataRepository spring data repository
     * @return ProjectRepository bean
     */
    @Bean
    public ProjectRepository projectRepository(SpringDataProjectRepository springDataRepository) {
        return new ProjectRepositoryImpl(springDataRepository);
    }

    /**
     * Registers ContextRepository.
     *
     * @param springDataRepository spring data repository
     * @return ContextRepository bean
     */
    @Bean
    public ContextRepository contextRepository(SpringDataContextSnapshotRepository springDataRepository) {
        return new ContextRepositoryImpl(springDataRepository);
    }

    /**
     * Registers AISessionRepository.
     *
     * @param springDataRepository spring data repository
     * @return AISessionRepository bean
     */
    @Bean
    public AISessionRepository aiSessionRepository(SpringDataAISessionRepository springDataRepository) {
        return new AISessionRepositoryImpl(springDataRepository);
    }

    /**
     * Registers JPA-backed KnowledgeGraphRepository as the Primary implementation.
     *
     * @param nodeRepository spring data node repository
     * @param relationshipRepository spring data relationship repository
     * @return KnowledgeGraphRepository bean
     */
    @Bean
    @Primary
    public KnowledgeGraphRepository jpaKnowledgeGraphRepository(
        SpringDataKnowledgeNodeRepository nodeRepository,
        SpringDataKnowledgeRelationshipRepository relationshipRepository
    ) {
        return new KnowledgeGraphRepositoryImpl(nodeRepository, relationshipRepository);
    }
}
