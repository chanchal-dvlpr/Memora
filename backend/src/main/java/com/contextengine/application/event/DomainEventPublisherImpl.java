package com.contextengine.application.event;

import com.contextengine.domain.event.*;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Concrete implementation of DomainEventPublisher in the application layer.
 * Maps concrete domain events to the Universal Event Frame and sends them to the EventDispatcher.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Event Subsystem (EV-SUB)
 * </p>
 */
public class DomainEventPublisherImpl implements DomainEventPublisher {

    private final EventDispatcher dispatcher;
    private final EventMonitor eventMonitor;
    private final EventSerializer serializer = new EventSerializer();

    /**
     * Constructs a DomainEventPublisherImpl with a default monitor.
     *
     * @param dispatcher event routing dispatcher
     */
    public DomainEventPublisherImpl(EventDispatcher dispatcher) {
        this(dispatcher, new EventMonitor());
    }

    /**
     * Constructs a DomainEventPublisherImpl with a custom monitor.
     *
     * @param dispatcher event routing dispatcher
     * @param eventMonitor telemetry metrics monitor
     */
    public DomainEventPublisherImpl(EventDispatcher dispatcher, EventMonitor eventMonitor) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "EventDispatcher must not be null");
        this.eventMonitor = Objects.requireNonNull(eventMonitor, "EventMonitor must not be null");
    }

    @Override
    public void publish(DomainEvent event) {
        if (event == null) return;

        eventMonitor.recordPublish();

        UUID eventId = UUID.randomUUID();
        Instant timestamp = event.occurredAt() != null ? event.occurredAt() : Instant.now();
        UUID correlationId = EventContext.correlationId();
        UUID causationId = EventContext.causationId();

        // Resolve Project ID and Topic
        UUID projectId = resolveProjectId(event);
        String topic = resolveTopic(event, projectId);
        Map<String, Object> payload = serializer.serialize(event);

        UniversalEventFrame envelope = new UniversalEventFrame(
            eventId,
            topic,
            timestamp,
            correlationId,
            causationId,
            projectId,
            1, // Event Schema Version
            payload
        );

        // Dispatch wrapped event
        dispatcher.dispatch(envelope);
    }

    private UUID resolveProjectId(DomainEvent event) {
        if (event instanceof ProjectRegistered ev) return ev.projectId().value();
        if (event instanceof ProjectScanned ev) return ev.projectId().value();
        if (event instanceof ModuleDiscovered ev) return ev.projectId().value();
        if (event instanceof FeatureCreated ev) return ev.projectId().value();
        if (event instanceof FeatureUpdated ev) return ev.projectId().value();
        if (event instanceof TaskCreated ev) return ev.projectId().value();
        if (event instanceof TaskCompleted ev) return ev.projectId().value();
        if (event instanceof DecisionRecorded ev) return ev.projectId().value();
        if (event instanceof DecisionApproved ev) return ev.projectId().value();
        if (event instanceof BugDetected ev) return ev.projectId().value();
        if (event instanceof ConstraintAdded ev) return ev.projectId().value();
        if (event instanceof AssumptionVerified ev) return ev.projectId().value();
        if (event instanceof DependencyUpdated ev) return ev.projectId().value();
        if (event instanceof ContextGenerated ev) return ev.projectId().value();
        if (event instanceof ContextRetrieved ev) return ev.projectId().value();
        if (event instanceof ContextSnapshotCreated ev) return ev.projectId().value();
        if (event instanceof KnowledgeGraphUpdated ev) return ev.projectId().value();
        if (event instanceof AIHandoffGenerated ev) return ev.projectId().value();

        // Default UUID for events without a target project ID
        return new UUID(0L, 0L);
    }

    private String resolveTopic(DomainEvent event, UUID projectId) {
        String base;
        if (event instanceof ProjectRegistered) {
            base = "project.registry.registered";
        } else if (event instanceof ProjectScanned) {
            base = "scanner.status.scanned";
        } else if (event instanceof ModuleDiscovered) {
            base = "workspace.module.discovered";
        } else if (event instanceof FeatureCreated) {
            base = "feature.tracker.created";
        } else if (event instanceof FeatureUpdated) {
            base = "feature.tracker.updated";
        } else if (event instanceof TaskCreated) {
            base = "task.tracker.created";
        } else if (event instanceof TaskCompleted) {
            base = "task.tracker.completed";
        } else if (event instanceof DecisionRecorded) {
            base = "decision.tracker.recorded";
        } else if (event instanceof DecisionApproved) {
            base = "decision.tracker.approved";
        } else if (event instanceof BugDetected) {
            base = "bug.tracker.detected";
        } else if (event instanceof ConstraintAdded) {
            base = "constraint.tracker.added";
        } else if (event instanceof AssumptionVerified) {
            base = "assumption.tracker.verified";
        } else if (event instanceof DependencyUpdated) {
            base = "dependency.tracker.updated";
        } else if (event instanceof ContextGenerated) {
            base = "context.orchestrator.generated";
        } else if (event instanceof ContextRetrieved) {
            base = "context.orchestrator.retrieved";
        } else if (event instanceof ContextSnapshotCreated) {
            base = "snapshot.orchestrator.created";
        } else if (event instanceof ContextVersionCreated) {
            base = "snapshot.orchestrator.version_created";
        } else if (event instanceof SearchExecuted) {
            base = "search.orchestrator.executed";
        } else if (event instanceof KnowledgeGraphUpdated) {
            base = "graph.compiler.updated";
        } else if (event instanceof AIHandoffGenerated) {
            base = "ai.orchestrator.handoff_generated";
        } else {
            base = "generic.domain.event";
        }

        return base + ".p_" + projectId.toString();
    }
}
