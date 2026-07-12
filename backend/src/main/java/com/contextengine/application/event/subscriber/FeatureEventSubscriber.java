package com.contextengine.application.event.subscriber;

import com.contextengine.application.event.EventDispatcher;
import com.contextengine.application.event.EventSubscriber;
import com.contextengine.application.event.UniversalEventFrame;
import com.contextengine.application.event.handler.FeatureCreatedHandler;
import java.util.Objects;

/**
 * Event subscriber routing feature created events.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Event Subsystem (EV-SUB)
 * </p>
 */
public class FeatureEventSubscriber implements EventSubscriber {

    private final FeatureCreatedHandler createdHandler;

    /**
     * Constructs a FeatureEventSubscriber and registers with L-Bus.
     *
     * @param dispatcher event dispatcher
     * @param createdHandler feature created handler
     */
    public FeatureEventSubscriber(EventDispatcher dispatcher, FeatureCreatedHandler createdHandler) {
        Objects.requireNonNull(dispatcher, "EventDispatcher must not be null");
        this.createdHandler = Objects.requireNonNull(createdHandler, "FeatureCreatedHandler must not be null");

        dispatcher.subscribe("feature.tracker.created.*", this);
    }

    @Override
    public void onEvent(UniversalEventFrame envelope) {
        Objects.requireNonNull(envelope, "UniversalEventFrame must not be null");
        createdHandler.handle(envelope);
    }
}
