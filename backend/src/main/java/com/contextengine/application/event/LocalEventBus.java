package com.contextengine.application.event;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * In-memory local event bus (L-Bus) routing published event frames to registered subscriber handlers
 * using hierarchical wildcard segment matching.
 * Extends the baseline routing with:
 * - Priority-allocated thread pools (Control, Processing, and Telemetry).
 * - Exponential backoff retry broker (up to 5 attempts).
 * - Dead-letter journal quarantine fallback.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Event Subsystem (EV-SUB)
 * </p>
 */
public class LocalEventBus implements EventDispatcher {

    private final Map<String, Set<EventSubscriber>> subscriptions = new ConcurrentHashMap<>();
    private final DeadLetterJournal deadLetterJournal;
    private final EventValidator validator;
    private final EventMonitor monitor;
    private final boolean async;

    // Isolated Priority-allocated executors
    private final ExecutorService controlExecutor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "lbus-control-pool");
        t.setDaemon(true);
        return t;
    });
    private final ExecutorService processingExecutor = Executors.newFixedThreadPool(
        Math.max(1, Runtime.getRuntime().availableProcessors() - 1),
        r -> {
            Thread t = new Thread(r, "lbus-processing-pool");
            t.setDaemon(true);
            return t;
        }
    );
    private final ExecutorService telemetryExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "lbus-telemetry-pool");
        t.setDaemon(true);
        return t;
    });

    /**
     * Constructs a LocalEventBus in synchronous mode by default.
     *
     * @param deadLetterJournal registry for failed events
     */
    public LocalEventBus(DeadLetterJournal deadLetterJournal) {
        this(deadLetterJournal, new EventValidator(), new EventMonitor(), false);
    }

    /**
     * Constructs a LocalEventBus with specified async mode.
     *
     * @param deadLetterJournal registry for failed events
     * @param async if true, executes dispatching asynchronously using priority-based thread pools
     */
    public LocalEventBus(DeadLetterJournal deadLetterJournal, boolean async) {
        this(deadLetterJournal, new EventValidator(), new EventMonitor(), async);
    }

    /**
     * Constructs a LocalEventBus with validation and monitoring components.
     *
     * @param deadLetterJournal registry for failed events
     * @param validator event validator component
     * @param monitor event telemetry monitor
     */
    public LocalEventBus(DeadLetterJournal deadLetterJournal, EventValidator validator, EventMonitor monitor) {
        this(deadLetterJournal, validator, monitor, false);
    }

    /**
     * Constructs a LocalEventBus with all dependencies.
     *
     * @param deadLetterJournal registry for failed events
     * @param validator event validator component
     * @param monitor event telemetry monitor
     * @param async if true, executes dispatching asynchronously
     */
    public LocalEventBus(DeadLetterJournal deadLetterJournal, EventValidator validator, EventMonitor monitor, boolean async) {
        this.deadLetterJournal = Objects.requireNonNull(deadLetterJournal, "DeadLetterJournal must not be null");
        this.validator = Objects.requireNonNull(validator, "EventValidator must not be null");
        this.monitor = Objects.requireNonNull(monitor, "EventMonitor must not be null");
        this.async = async;
    }

    @Override
    public void dispatch(UniversalEventFrame envelope) {
        Objects.requireNonNull(envelope, "UniversalEventFrame must not be null");
        validator.validate(envelope);

        String topic = envelope.topic();

        for (Map.Entry<String, Set<EventSubscriber>> entry : subscriptions.entrySet()) {
            String pattern = entry.getKey();
            if (matches(pattern, topic)) {
                for (EventSubscriber sub : entry.getValue()) {
                    if (async) {
                        ExecutorService executor = resolveExecutor(topic);
                        executor.submit(() -> invokeWithRetry(sub, envelope));
                    } else {
                        invokeWithRetry(sub, envelope);
                    }
                }
            }
        }
    }

    private ExecutorService resolveExecutor(String topic) {
        if (topic.startsWith("project.") || topic.startsWith("config.")) {
            return controlExecutor;
        } else if (topic.startsWith("workspace.") || topic.startsWith("parser.") || topic.startsWith("graph.")) {
            return processingExecutor;
        } else {
            return telemetryExecutor;
        }
    }

    private void invokeWithRetry(EventSubscriber sub, UniversalEventFrame envelope) {
        int maxAttempts = 5;
        int baseDelayMs = 10;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                sub.onEvent(envelope);
                return; // processing succeeded
            } catch (Exception e) {
                if (attempt == maxAttempts) {
                    monitor.recordFailure();
                    deadLetterJournal.quarantine(envelope, "Retries exhausted. Error: " + e.getMessage());
                    throw new EventException("Subscriber final processing failure", e);
                }
                long delay = (long) (baseDelayMs * Math.pow(2, attempt));
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new EventException("Event retry sequence interrupted", ie);
                }
            }
        }
    }

    @Override
    public void subscribe(String topicPattern, EventSubscriber subscriber) {
        Objects.requireNonNull(topicPattern, "Topic pattern must not be null");
        Objects.requireNonNull(subscriber, "Subscriber must not be null");

        subscriptions.computeIfAbsent(topicPattern.trim(), k -> new CopyOnWriteArraySet<>())
            .add(subscriber);
    }

    @Override
    public void unsubscribe(String topicPattern, EventSubscriber subscriber) {
        Objects.requireNonNull(topicPattern, "Topic pattern must not be null");
        Objects.requireNonNull(subscriber, "Subscriber must not be null");

        Set<EventSubscriber> subs = subscriptions.get(topicPattern.trim());
        if (subs != null) {
            subs.remove(subscriber);
            if (subs.isEmpty()) {
                subscriptions.remove(topicPattern.trim());
            }
        }
    }

    /**
     * Gracefully shuts down priority thread executors.
     */
    public void shutdown() {
        controlExecutor.shutdown();
        processingExecutor.shutdown();
        telemetryExecutor.shutdown();
        try {
            if (!controlExecutor.awaitTermination(2, TimeUnit.SECONDS)) controlExecutor.shutdownNow();
            if (!processingExecutor.awaitTermination(2, TimeUnit.SECONDS)) processingExecutor.shutdownNow();
            if (!telemetryExecutor.awaitTermination(2, TimeUnit.SECONDS)) telemetryExecutor.shutdownNow();
        } catch (InterruptedException e) {
            controlExecutor.shutdownNow();
            processingExecutor.shutdownNow();
            telemetryExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private boolean matches(String pattern, String topic) {
        if ("*".equals(pattern) || pattern.equals(topic)) {
            return true;
        }
        String[] patternSegments = pattern.split("\\.");
        String[] topicSegments = topic.split("\\.");
        if (patternSegments.length != topicSegments.length) {
            return false;
        }
        for (int i = 0; i < patternSegments.length; i++) {
            if (!patternSegments[i].equals("*") && !patternSegments[i].equals(topicSegments[i])) {
                return false;
            }
        }
        return true;
    }

    public Map<String, Collection<EventSubscriber>> activeSubscriptions() {
        Map<String, Collection<EventSubscriber>> view = new ConcurrentHashMap<>();
        subscriptions.forEach((k, v) -> view.put(k, Collections.unmodifiableCollection(v)));
        return Collections.unmodifiableMap(view);
    }
}
