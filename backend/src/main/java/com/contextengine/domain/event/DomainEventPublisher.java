package com.contextengine.domain.event;

/**
 * Domain-level outbound port interface for publishing Domain Events.
 * <p>
 * Bounded Context: Core Domain
 * Related Subsystem: Event Subsystem (EV-SUB)
 * </p>
 */
public interface DomainEventPublisher {

    /**
     * Publishes a domain event.
     *
     * @param event the domain event instance to publish
     */
    void publish(DomainEvent event);
}
