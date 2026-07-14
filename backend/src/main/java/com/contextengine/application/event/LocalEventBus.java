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
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.UUID;
import org.slf4j.MDC;

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
    private final ThreadPoolExecutor controlExecutor = new ThreadPoolExecutor(
        2, 2, 0L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(),
        r -> {
            Thread t = new Thread(r, "lbus-control-pool");
            t.setDaemon(true);
            return t;
        }
    );
    private final ThreadPoolExecutor processingExecutor = new ThreadPoolExecutor(
        Math.max(1, Runtime.getRuntime().availableProcessors() - 1),
        Math.max(1, Runtime.getRuntime().availableProcessors() - 1),
        0L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(),
        r -> {
            Thread t = new Thread(r, "lbus-processing-pool");
            t.setDaemon(true);
            return t;
        }
    );
    private final ThreadPoolExecutor telemetryExecutor = new ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(),
        r -> {
            Thread t = new Thread(r, "lbus-telemetry-pool");
            t.setDaemon(true);
            return t;
        }
    );

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
                        
                        // Capture correlation, causation, trace, and span context
                        final UUID correlationId = EventContext.correlationId();
                        final UUID causationId = EventContext.causationId();
                        final UUID traceId = EventContext.traceId();
                        final UUID spanId = EventContext.spanId();
                        
                        final String mdcCorrelationId = MDC.get("correlationId");
                        final String mdcRequestId = MDC.get("requestId");
                        final String mdcTraceId = MDC.get("traceId");
                        final String mdcSpanId = MDC.get("spanId");

                        executor.submit(() -> {
                            // Bind context to worker thread
                            EventContext.setCorrelationId(correlationId);
                            EventContext.setCausationId(causationId);
                            EventContext.setTraceId(traceId);
                            EventContext.setSpanId(spanId);

                            if (mdcCorrelationId != null) MDC.put("correlationId", mdcCorrelationId);
                            if (mdcRequestId != null) MDC.put("requestId", mdcRequestId);
                            if (mdcTraceId != null) MDC.put("traceId", mdcTraceId);
                            if (mdcSpanId != null) MDC.put("spanId", mdcSpanId);

                            try {
                                invokeWithRetry(sub, envelope);
                            } finally {
                                EventContext.clear();
                                MDC.clear();
                            }
                        });
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
        long start = System.currentTimeMillis();
        boolean success = false;

        try {
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    sub.onEvent(envelope);
                    success = true;
                    return; // processing succeeded
                } catch (Exception e) {
                    if (attempt == maxAttempts) {
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
        } finally {
            long duration = System.currentTimeMillis() - start;
            // Record to monitor if it's not the monitor itself (to avoid self-referential loop)
            if (sub != monitor) {
                monitor.recordProcessing(duration, success);
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

    public long getQueueBacklog() {
        long backlog = 0;
        backlog += controlExecutor.getQueue().size();
        backlog += processingExecutor.getQueue().size();
        backlog += telemetryExecutor.getQueue().size();
        return backlog;
    }

    public int getActiveSubscribers() {
        int count = 0;
        for (Set<EventSubscriber> subs : subscriptions.values()) {
            count += subs.size();
        }
        return count;
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
