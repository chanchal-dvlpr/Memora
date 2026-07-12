package com.contextengine.application.event;

import com.contextengine.application.event.handler.*;
import com.contextengine.application.event.subscriber.*;
import com.contextengine.application.port.EventJournal;
import com.contextengine.application.service.ProjectApplicationService;
import com.contextengine.domain.event.DomainEventPublisher;
import com.contextengine.persistence.repository.EventPersistenceAdapter;
import com.contextengine.persistence.repository.SpringDataEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration class registering Event System infrastructure beans.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Event Subsystem (EV-SUB)
 * </p>
 */
@Configuration
public class EventConfiguration {

    /**
     * Registers EventValidator.
     *
     * @return EventValidator bean
     */
    @Bean
    public EventValidator eventValidator() {
        return new EventValidator();
    }

    /**
     * Registers EventMonitor.
     *
     * @return EventMonitor bean
     */
    @Bean
    public EventMonitor eventMonitor() {
        return new EventMonitor();
    }

    /**
     * Registers DeadLetterJournal.
     *
     * @return DeadLetterJournal bean
     */
    @Bean
    public DeadLetterJournal deadLetterJournal() {
        return new DeadLetterJournal();
    }

    /**
     * Registers L-Bus EventDispatcher.
     *
     * @param deadLetterJournal dead-letter journal registry
     * @param validator event validator component
     * @param monitor event telemetry monitor
     * @return EventDispatcher bean
     */
    @Bean
    public EventDispatcher eventDispatcher(
        DeadLetterJournal deadLetterJournal,
        EventValidator validator,
        EventMonitor monitor
    ) {
        LocalEventBus bus = new LocalEventBus(deadLetterJournal, validator, monitor);
        monitor.register(bus);
        return bus;
    }

    /**
     * Registers DomainEventPublisher interface implementation.
     *
     * @param dispatcher event dispatcher
     * @param monitor event telemetry monitor
     * @return DomainEventPublisher bean
     */
    @Bean
    public DomainEventPublisher domainEventPublisher(EventDispatcher dispatcher, EventMonitor monitor) {
        return new DomainEventPublisherImpl(dispatcher, monitor);
    }

    /**
     * Registers EventJournal persistent logging store.
     *
     * @param repository Spring Data Event repository
     * @param objectMapper Jackson Mapper
     * @return EventJournal bean
     */
    @Bean
    public EventJournal eventJournal(SpringDataEventRepository repository, ObjectMapper objectMapper) {
        return new EventPersistenceAdapter(repository, objectMapper);
    }

    /**
     * Registers EventReplayService.
     *
     * @param journal persistence store port
     * @param dispatcher event dispatcher
     * @param monitor telemetry monitor
     * @return EventReplayService bean
     */
    @Bean
    public EventReplayService eventReplayService(
        EventJournal journal,
        EventDispatcher dispatcher,
        EventMonitor monitor
    ) {
        return new EventReplayService(journal, dispatcher, monitor);
    }

    // Handlers Bean Definitions

    @Bean
    public ProjectRegisteredHandler projectRegisteredHandler(ProjectApplicationService projectService) {
        return new ProjectRegisteredHandler(projectService);
    }

    @Bean
    public ProjectScannedHandler projectScannedHandler() {
        return new ProjectScannedHandler();
    }

    @Bean
    public FeatureCreatedHandler featureCreatedHandler() {
        return new FeatureCreatedHandler();
    }

    @Bean
    public ContextGeneratedHandler contextGeneratedHandler() {
        return new ContextGeneratedHandler();
    }

    @Bean
    public KnowledgeGraphUpdatedHandler knowledgeGraphUpdatedHandler() {
        return new KnowledgeGraphUpdatedHandler();
    }

    // Subscribers Bean Definitions

    @Bean
    public ProjectEventSubscriber projectEventSubscriber(
        EventDispatcher dispatcher,
        ProjectRegisteredHandler registeredHandler,
        ProjectScannedHandler scannedHandler
    ) {
        return new ProjectEventSubscriber(dispatcher, registeredHandler, scannedHandler);
    }

    @Bean
    public FeatureEventSubscriber featureEventSubscriber(
        EventDispatcher dispatcher,
        FeatureCreatedHandler createdHandler
    ) {
        return new FeatureEventSubscriber(dispatcher, createdHandler);
    }

    @Bean
    public ContextEventSubscriber contextEventSubscriber(
        EventDispatcher dispatcher,
        ContextGeneratedHandler generatedHandler
    ) {
        return new ContextEventSubscriber(dispatcher, generatedHandler);
    }

    @Bean
    public GraphEventSubscriber graphEventSubscriber(
        EventDispatcher dispatcher,
        KnowledgeGraphUpdatedHandler updatedHandler
    ) {
        return new GraphEventSubscriber(dispatcher, updatedHandler);
    }
}
