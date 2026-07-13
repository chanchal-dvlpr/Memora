package com.contextengine.application.knowledge;

import java.util.Objects;

/**
 * Tracks the state machine phases for building and resolving the Knowledge Graph.
 * <p>
 * Bounded Context: Knowledge Graph
 * Related Subsystem: Knowledge Graph Engine (KG-ENG)
 * Reference: Functional Requirement FR-016 (Knowledge Graph Engine) Section 9
 * </p>
 */
public class KnowledgeSession {

    /**
     * Operational states of the Knowledge Graph construction lifecycle.
     */
    public enum State {
        /** Isolated schema boundary allocated; code scanners un-initialized. */
        CREATED,
        /** Ingestion channels stream file paths, namespaces, AST trees, etc. */
        BUILDING,
        /** Ingestion pauses; verification processes audit referential dependencies. */
        VALIDATING,
        /** Graph successfully loaded, validated, and ready for queries. */
        ACTIVE,
        /** Error state. */
        FAULTED
    }

    private State state;
    private final String sessionId;

    /**
     * Constructs a KnowledgeSession starting in the CREATED state.
     *
     * @param sessionId unique session identifier
     */
    public KnowledgeSession(String sessionId) {
        this.sessionId = Objects.requireNonNull(sessionId, "SessionId must not be null");
        this.state = State.CREATED;
    }

    /**
     * Returns the session ID.
     *
     * @return session ID string
     */
    public String sessionId() {
        return sessionId;
    }

    /**
     * Returns the current state of the session.
     *
     * @return current State
     */
    public State state() {
        return state;
    }

    /**
     * Transitions the session to a new state.
     *
     * @param newState the target state
     * @throws IllegalStateException if transitioning from FAULTED to states other than CREATED
     */
    public synchronized void transitionTo(State newState) {
        Objects.requireNonNull(newState, "NewState must not be null");
        if (state == State.FAULTED && newState != State.CREATED) {
            throw new IllegalStateException("Cannot transition from FAULTED to " + newState);
        }
        this.state = newState;
    }
}
