package com.contextengine.application.event.subscriber;

import com.contextengine.application.event.EventDispatcher;
import com.contextengine.application.event.EventSubscriber;
import com.contextengine.application.event.UniversalEventFrame;
import com.contextengine.application.event.handler.ProjectRegisteredHandler;
import com.contextengine.application.event.handler.ProjectScannedHandler;
import java.util.Objects;

/**
 * Event subscriber routing project registration and scanner status events.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Event Subsystem (EV-SUB)
 * </p>
 */
public class ProjectEventSubscriber implements EventSubscriber {

    private final ProjectRegisteredHandler registeredHandler;
    private final ProjectScannedHandler scannedHandler;

    /**
     * Constructs a ProjectEventSubscriber and subscribes to project/scanner patterns.
     *
     * @param dispatcher event bus dispatcher
     * @param registeredHandler registration handler
     * @param scannedHandler scanner handler
     */
    public ProjectEventSubscriber(
        EventDispatcher dispatcher,
        ProjectRegisteredHandler registeredHandler,
        ProjectScannedHandler scannedHandler
    ) {
        Objects.requireNonNull(dispatcher, "EventDispatcher must not be null");
        this.registeredHandler = Objects.requireNonNull(registeredHandler, "ProjectRegisteredHandler must not be null");
        this.scannedHandler = Objects.requireNonNull(scannedHandler, "ProjectScannedHandler must not be null");

        dispatcher.subscribe("project.registry.registered.*", this);
        dispatcher.subscribe("scanner.status.scanned.*", this);
    }

    @Override
    public void onEvent(UniversalEventFrame envelope) {
        Objects.requireNonNull(envelope, "UniversalEventFrame must not be null");
        if (envelope.topic().startsWith("project.registry.registered")) {
            registeredHandler.handle(envelope);
        } else if (envelope.topic().startsWith("scanner.status.scanned")) {
            scannedHandler.handle(envelope);
        }
    }
}
