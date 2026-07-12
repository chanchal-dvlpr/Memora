package com.contextengine.application.event.subscriber;

import com.contextengine.application.event.EventDispatcher;
import com.contextengine.application.event.EventSubscriber;
import com.contextengine.application.event.UniversalEventFrame;
import com.contextengine.application.event.handler.KnowledgeGraphUpdatedHandler;
import java.util.Objects;

/**
 * Event subscriber routing Knowledge Graph update events.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Event Subsystem (EV-SUB)
 * </p>
 */
public class GraphEventSubscriber implements EventSubscriber {

    private final KnowledgeGraphUpdatedHandler updatedHandler;

    /**
     * Constructs a GraphEventSubscriber and registers with L-Bus.
     *
     * @param dispatcher event dispatcher
     * @param updatedHandler knowledge graph updated handler
     */
    public GraphEventSubscriber(EventDispatcher dispatcher, KnowledgeGraphUpdatedHandler updatedHandler) {
        Objects.requireNonNull(dispatcher, "EventDispatcher must not be null");
        this.updatedHandler = Objects.requireNonNull(updatedHandler, "KnowledgeGraphUpdatedHandler must not be null");

        dispatcher.subscribe("graph.compiler.updated.*", this);
    }

    @Override
    public void onEvent(UniversalEventFrame envelope) {
        Objects.requireNonNull(envelope, "UniversalEventFrame must not be null");
        updatedHandler.handle(envelope);
    }
}
