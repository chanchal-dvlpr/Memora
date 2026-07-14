package com.contextengine.api.advice;

import java.util.List;

/**
 * Standardized system error response envelope payload.
 * <p>
 * Bounded Context: REST Presentation Layer
 * Reference: Section 5.18.2 Error Contracts (Schematic Representation)
 * </p>
 */
public class ErrorPayload {

    private String category;
    private String code;
    private String message;
    private String correlationId;
    private String timestamp;
    private List<ErrorFieldDetails> details;

    /**
     * Default constructor for serialization.
     */
    public ErrorPayload() {
    }

    /**
     * Constructs an ErrorPayload.
     *
     * @param category the broad category of the failure (e.g. VALIDATION, SYSTEM)
     * @param code specific machine-readable error classification code
     * @param message human-readable detailed explanation
     * @param correlationId unique tracking trace identifier for correlation logs
     * @param timestamp occurrence timestamp
     * @param details list of specific field validation failures
     */
    public ErrorPayload(String category, String code, String message, String correlationId, String timestamp, List<ErrorFieldDetails> details) {
        this.category = category;
        this.code = code;
        this.message = message;
        this.correlationId = correlationId;
        this.timestamp = timestamp;
        this.details = details;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public List<ErrorFieldDetails> getDetails() {
        return details;
    }

    public void setDetails(List<ErrorFieldDetails> details) {
        this.details = details;
    }
}
