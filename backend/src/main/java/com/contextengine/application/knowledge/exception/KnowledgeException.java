package com.contextengine.application.knowledge.exception;

import com.contextengine.application.exception.ApplicationException;

/**
 * Exception class representing knowledge processing and graph construction failures.
 * <p>
 * Bounded Context: Knowledge Graph
 * Related Subsystem: Knowledge Graph Engine (KG-ENG)
 * Reference: Functional Requirement FR-016 (Knowledge Graph Engine) Section 16
 * </p>
 */
public class KnowledgeException extends ApplicationException {

    private final String errorCode;

    /**
     * Constructs a KnowledgeException.
     *
     * @param message the detailed failure message
     * @param errorCode the error code identifier (e.g. ERR_GRAPH_INVALID_CONFIG)
     */
    public KnowledgeException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructs a KnowledgeException with a cause.
     *
     * @param message the detailed failure message
     * @param errorCode the error code identifier
     * @param cause the causing exception
     */
    public KnowledgeException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Returns the error code identifier.
     *
     * @return the error code
     */
    public String errorCode() {
        return errorCode;
    }
}
