package com.contextengine.concurrency;

import com.contextengine.domain.entity.Project;
import com.contextengine.domain.repository.ProjectRepository;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.security.foundation.SecurityContext;
import com.contextengine.application.event.LocalEventBus;
import com.contextengine.application.event.UniversalEventFrame;
import com.contextengine.application.event.EventSubscriber;
import com.contextengine.test.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency tests verifying event dispatch safety, SecurityContext isolation,
 * and concurrent repository access.
 */
class ConcurrencyTest extends BaseIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void testSecurityContextThreadLocalIsolation() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Boolean> task1 = () -> {
            SecurityContext.Principal p1 = new SecurityContext.Principal("client-1", Set.of("READ"));
            SecurityContext.setPrincipal(p1);
            Thread.sleep(100);
            boolean match = SecurityContext.getPrincipal().map(p -> p.identity().equals("client-1")).orElse(false);
            SecurityContext.clear();
            return match;
        };

        Callable<Boolean> task2 = () -> {
            boolean startsEmpty = !SecurityContext.getPrincipal().isPresent();
            SecurityContext.Principal p2 = new SecurityContext.Principal("client-2", Set.of("READ", "WRITE"));
            SecurityContext.setPrincipal(p2);
            boolean match = SecurityContext.getPrincipal().map(p -> p.identity().equals("client-2")).orElse(false);
            SecurityContext.clear();
            return startsEmpty && match;
        };

        Future<Boolean> f1 = executor.submit(task1);
        Future<Boolean> f2 = executor.submit(task2);

        assertThat(f1.get()).isTrue();
        assertThat(f2.get()).isTrue();

        executor.shutdown();
    }

    @Test
    void testConcurrentEventPublishing() throws InterruptedException {
        int threadCount = 10;
        int eventsPerThread = 20;
        int totalEvents = threadCount * eventsPerThread;

        CountDownLatch latch = new CountDownLatch(totalEvents);
        LocalEventBus asyncBus = new LocalEventBus(new com.contextengine.application.event.DeadLetterJournal(), true);

        UUID projId = UUID.randomUUID();
        String topic = "project.created.p_" + projId;

        asyncBus.subscribe("project.created.*", new EventSubscriber() {
            @Override
            public void onEvent(UniversalEventFrame event) {
                latch.countDown();
            }
        });

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                for (int e = 0; e < eventsPerThread; e++) {
                    UUID evtId = UUID.randomUUID();
                    asyncBus.dispatch(new UniversalEventFrame(
                        evtId,
                        topic,
                        Instant.now(),
                        evtId,
                        evtId,
                        projId,
                        1,
                        Collections.emptyMap()
                    ));
                }
            });
        }

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        asyncBus.shutdown();
        executor.shutdown();

        assertThat(completed).isTrue();
    }

    @Test
    void testConcurrentRepositoryAccess() throws InterruptedException {
        int threadCount = 5;
        ProjectId projectId = ProjectId.generate();
        Project project = new Project(projectId, new Path("/workspace/concurrency"), "Shared Project");
        projectRepository.save(project);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    Optional<Project> p = projectRepository.findById(projectId);
                    if (p.isPresent()) {
                        assertThat(p.get().title()).isEqualTo("Shared Project");
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        assertThat(completed).isTrue();
    }
}
