package com.contextengine.application.event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Passive diagnostic telemetry monitoring component tracking event bus rates and metrics.
 * Implements EventSubscriber to capture metrics without interfering with processing loops.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Monitoring Subsystem (MN-SUB)
 * </p>
 */
public class EventMonitor implements EventSubscriber {

    private final AtomicLong publishedCount = new AtomicLong(0);
    private final AtomicLong dispatchedCount = new AtomicLong(0);
    private final AtomicLong replayedCount = new AtomicLong(0);
    private final AtomicLong failedCount = new AtomicLong(0);

    private final Map<String, AtomicLong> topicDispatchedCounts = new ConcurrentHashMap<>();

    /**
     * Constructs the EventMonitor.
     */
    public EventMonitor() {
        // Constructor matches standard dependency injection pattern
    }

    /**
     * Subscribes this monitor instance to all dispatcher events.
     *
     * @param dispatcher event bus dispatcher
     */
    public void register(EventDispatcher dispatcher) {
        dispatcher.subscribe("*", this);
    }

    @Override
    public void onEvent(UniversalEventFrame envelope) {
        dispatchedCount.incrementAndGet();
        topicDispatchedCounts.computeIfAbsent(envelope.topic(), k -> new AtomicLong(0))
            .incrementAndGet();
    }

    /**
     * Increments the count of successfully published events.
     */
    public void recordPublish() {
        publishedCount.incrementAndGet();
    }

    /**
     * Increments the count of replayed events.
     */
    public void recordReplay() {
        replayedCount.incrementAndGet();
    }

    /**
     * Increments the count of failed/quarantined events.
     */
    public void recordFailure() {
        failedCount.incrementAndGet();
    }

    public long getPublishedCount() {
        return publishedCount.get();
    }

    public long getDispatchedCount() {
        return dispatchedCount.get();
    }

    public long getReplayedCount() {
        return replayedCount.get();
    }

    public long getFailedCount() {
        return failedCount.get();
    }

    /**
     * Returns a breakdown of dispatched event counts by topic.
     *
     * @return unmodifiable map view
     */
    public Map<String, Long> getTopicBreakdown() {
        Map<String, Long> breakdown = new ConcurrentHashMap<>();
        topicDispatchedCounts.forEach((k, v) -> breakdown.put(k, v.get()));
        return Map.copyOf(breakdown);
    }

    /**
     * Resets all diagnostic metrics to zero.
     */
    public void reset() {
        publishedCount.set(0);
        dispatchedCount.set(0);
        replayedCount.set(0);
        failedCount.set(0);
        topicDispatchedCounts.clear();
    }
}
