package com.contextengine.application.event;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventExtensionsTest {

    private static final String VALID_UUID = "451df677-742a-43d9-95dc-100281b37b60";

    @Test
    void testAsyncDispatchAndThreadPoolNaming() throws InterruptedException {
        DeadLetterJournal journal = new DeadLetterJournal();
        LocalEventBus eventBus = new LocalEventBus(journal, true);

        CountDownLatch latch = new CountDownLatch(1);
        final String[] threadName = new String[1];

        eventBus.subscribe("workspace.file.discovered.p_" + VALID_UUID, envelope -> {
            threadName[0] = Thread.currentThread().getName();
            latch.countDown();
        });

        UniversalEventFrame envelope = new UniversalEventFrame(
            UUID.randomUUID(),
            "workspace.file.discovered.p_" + VALID_UUID,
            Instant.now(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.fromString(VALID_UUID),
            1,
            Map.of()
        );

        eventBus.dispatch(envelope);

        boolean finished = latch.await(2, TimeUnit.SECONDS);
        assertThat(finished).isTrue();
        assertThat(threadName[0]).startsWith("lbus-processing-pool");

        eventBus.shutdown();
    }

    @Test
    void testTransientFailureRetriesAndSucceeds() {
        DeadLetterJournal journal = new DeadLetterJournal();
        LocalEventBus eventBus = new LocalEventBus(journal, false); // sync for deterministic assert

        AtomicInteger attempts = new AtomicInteger(0);

        eventBus.subscribe("feature.tracker.created.p_" + VALID_UUID, envelope -> {
            int current = attempts.incrementAndGet();
            if (current < 3) {
                throw new RuntimeException("Transient db error");
            }
        });

        UniversalEventFrame envelope = new UniversalEventFrame(
            UUID.randomUUID(),
            "feature.tracker.created.p_" + VALID_UUID,
            Instant.now(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.fromString(VALID_UUID),
            1,
            Map.of()
        );

        eventBus.dispatch(envelope);

        assertThat(attempts.get()).isEqualTo(3);
        assertThat(journal.quarantinedEvents()).isEmpty();
    }

    @Test
    void testPersistentFailureQuarantinesToDeadLetter() {
        DeadLetterJournal journal = new DeadLetterJournal();
        LocalEventBus eventBus = new LocalEventBus(journal, false);

        AtomicInteger attempts = new AtomicInteger(0);

        eventBus.subscribe("task.tracker.completed.p_" + VALID_UUID, envelope -> {
            attempts.incrementAndGet();
            throw new RuntimeException("Fatal subscriber failure");
        });

        UniversalEventFrame envelope = new UniversalEventFrame(
            UUID.randomUUID(),
            "task.tracker.completed.p_" + VALID_UUID,
            Instant.now(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.fromString(VALID_UUID),
            1,
            Map.of()
        );

        assertThatThrownBy(() -> eventBus.dispatch(envelope))
            .isInstanceOf(EventException.class)
            .hasMessageContaining("Subscriber final processing failure");

        assertThat(attempts.get()).isEqualTo(5);
        assertThat(journal.quarantinedEvents()).hasSize(1);
        assertThat(journal.quarantinedEvents().iterator().next().eventId()).isEqualTo(envelope.eventId());
    }
}
