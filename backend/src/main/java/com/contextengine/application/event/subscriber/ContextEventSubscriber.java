package com.contextengine.application.event.subscriber;

import com.contextengine.application.event.EventDispatcher;
import com.contextengine.application.event.EventSubscriber;
import com.contextengine.application.event.UniversalEventFrame;
import com.contextengine.application.event.handler.ContextGeneratedHandler;
import java.util.Objects;

/**
 * Event subscriber routing context compilation events.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Event Subsystem (EV-SUB)
 * </p>
 */
public class ContextEventSubscriber implements EventSubscriber {

    private final ContextGeneratedHandler generatedHandler;

    /**
     * Constructs a ContextEventSubscriber and registers with L-Bus.
     *
     * @param dispatcher event dispatcher
     * @param generatedHandler context generated handler
     */
    public ContextEventSubscriber(EventDispatcher dispatcher, ContextGeneratedHandler generatedHandler) {
        Objects.requireNonNull(dispatcher, "EventDispatcher must not be null");
        this.generatedHandler = Objects.requireNonNull(generatedHandler, "ContextGeneratedHandler must not be null");

        dispatcher.subscribe("context.orchestrator.generated.*", this);
    }

    @Override
    public void onEvent(UniversalEventFrame envelope) {
        Objects.requireNonNull(envelope, "UniversalEventFrame must not be null");
        generatedHandler.handle(envelope);
    }
}
