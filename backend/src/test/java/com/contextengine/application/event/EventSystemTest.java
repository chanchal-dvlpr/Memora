package com.contextengine.application.event;

import com.contextengine.domain.event.DomainEventPublisher;
import com.contextengine.domain.event.ProjectRegistered;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class EventSystemTest {

    private LocalEventBus eventBus;
    private DomainEventPublisher publisher;

    @BeforeEach
    void setUp() {
        eventBus = new LocalEventBus(new DeadLetterJournal());
        publisher = new DomainEventPublisherImpl(eventBus);
        EventContext.clear();
    }

    @AfterEach
    void tearDown() {
        EventContext.clear();
    }

    @Test
    void testEventPublicationAndWildcardRouting() {
        ProjectId projectId = ProjectId.generate();
        Path rootPath = new Path("/path/to/project");
        ProjectRegistered domainEvent = new ProjectRegistered(projectId, rootPath, Instant.now());

        List<UniversalEventFrame> receivedEnvelopes = new ArrayList<>();
        EventSubscriber subscriber = receivedEnvelopes::add;

        // Register with exact match
        String exactPattern = "project.registry.registered.p_" + projectId.value().toString();
        eventBus.subscribe(exactPattern, subscriber);

        // Register with wildcard match on segment 3
        String wildcardPattern1 = "project.registry.*.p_" + projectId.value().toString();
        eventBus.subscribe(wildcardPattern1, subscriber);

        // Register with wrong pattern (should not receive event)
        String wrongPattern = "workspace.file.discovered.p_" + projectId.value().toString();
        eventBus.subscribe(wrongPattern, subscriber);

        // Define trace context
        UUID testCorrelationId = UUID.randomUUID();
        UUID testCausationId = UUID.randomUUID();
        EventContext.setCorrelationId(testCorrelationId);
        EventContext.setCausationId(testCausationId);

        // Publish event
        publisher.publish(domainEvent);

        // Should be received twice (one from exact match, one from wildcard match)
        assertThat(receivedEnvelopes).hasSize(2);

        UniversalEventFrame firstEnvelope = receivedEnvelopes.get(0);
        assertThat(firstEnvelope.correlationId()).isEqualTo(testCorrelationId);
        assertThat(firstEnvelope.causationId()).isEqualTo(testCausationId);
        assertThat(firstEnvelope.projectId()).isEqualTo(projectId.value());
        assertThat(firstEnvelope.version()).isEqualTo(1);
        assertThat(firstEnvelope.payload().get("absoluteRootPath")).isEqualTo("/path/to/project");
        assertThat(firstEnvelope.payload().get("projectId")).isEqualTo(projectId.value().toString());

        // Unsubscribe one pattern
        eventBus.unsubscribe(exactPattern, subscriber);
        receivedEnvelopes.clear();

        // Publish again
        publisher.publish(domainEvent);

        // Should only be received once from the remaining wildcard match
        assertThat(receivedEnvelopes).hasSize(1);
    }
}
