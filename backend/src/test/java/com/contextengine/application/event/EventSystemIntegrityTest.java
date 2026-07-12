package com.contextengine.application.event;

import com.contextengine.application.port.EventJournal;
import com.contextengine.domain.event.ProjectRegistered;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class EventSystemIntegrityTest {

    @Autowired
    private EventJournal eventJournal;

    @Autowired
    private EventDispatcher eventDispatcher;

    @Autowired
    private EventMonitor eventMonitor;

    @Autowired
    private EventReplayService eventReplayService;

    @BeforeEach
    void setUp() {
        eventMonitor.reset();
    }

    @Test
    void testPersistentLoggingAndSequenceOrdering() {
        UUID projectId = UUID.randomUUID();
        UniversalEventFrame first = new UniversalEventFrame(
            UUID.randomUUID(),
            "project.registry.registered.p_" + projectId,
            Instant.now(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            projectId,
            1,
            Map.of("testKey", "value1")
        );

        UniversalEventFrame second = new UniversalEventFrame(
            UUID.randomUUID(),
            "scanner.status.scanned.p_" + projectId,
            Instant.now(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            projectId,
            1,
            Map.of("testKey", "value2")
        );

        eventJournal.persist(first);
        eventJournal.persist(second);

        List<UniversalEventFrame> retrieved = eventJournal.retrieveEvents(projectId, 1L);
        assertThat(retrieved).hasSize(2);
        assertThat(retrieved.get(0).eventId()).isEqualTo(first.eventId());
        assertThat(retrieved.get(1).eventId()).isEqualTo(second.eventId());
        
        // Assert sequence numbers are sequential
        assertThat(eventJournal.getLatestSequenceNum(projectId)).isEqualTo(2L);
    }

    @Test
    void testReplayChronologicalFlow() {
        UUID projectId = UUID.randomUUID();
        UniversalEventFrame envelope = new UniversalEventFrame(
            UUID.randomUUID(),
            "feature.tracker.created.p_" + projectId,
            Instant.now(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            projectId,
            1,
            Map.of("featureId", UUID.randomUUID().toString())
        );

        eventJournal.persist(envelope);

        List<UniversalEventFrame> received = new ArrayList<>();
        eventDispatcher.subscribe("feature.tracker.created.*", received::add);

        eventReplayService.replayAll(projectId);

        assertThat(received).hasSize(1);
        assertThat(received.get(0).eventId()).isEqualTo(envelope.eventId());
        assertThat(eventMonitor.getReplayedCount()).isEqualTo(1);
    }

    @Test
    void testEventMonitorTelemetryRates() {
        UUID projectId = UUID.randomUUID();
        UniversalEventFrame envelope = new UniversalEventFrame(
            UUID.randomUUID(),
            "graph.compiler.updated.p_" + projectId,
            Instant.now(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            projectId,
            1,
            Map.of()
        );

        eventDispatcher.dispatch(envelope);

        // Dispatched should increment
        assertThat(eventMonitor.getDispatchedCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void testEventValidatorRejectsInvalidTopics() {
        // Missing project suffix in topic
        UniversalEventFrame invalidTopic = new UniversalEventFrame(
            UUID.randomUUID(),
            "project.registry.registered", // missing project suffix
            Instant.now(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            1,
            Map.of()
        );

        assertThatThrownBy(() -> eventDispatcher.dispatch(invalidTopic))
            .isInstanceOf(EventException.class)
            .hasMessageContaining("topic must contain project namespace prefix");
    }
}
