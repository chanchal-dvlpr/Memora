package com.contextengine.api.advice;

/**
 * Stores target property validation error coordinates and description details.
 * <p>
 * Bounded Context: REST Presentation Layer
 * Reference: Section 5.18.2 Error Contracts (Schematic Representation)
 * </p>
 */
public class ErrorFieldDetails {

    private String field;
    private String issue;

    /**
     * Default constructor for serialization.
     */
    public ErrorFieldDetails() {
    }

    /**
     * Constructs a field detail record.
     *
     * @param field the name of the request property field violating the constraints
     * @param issue description of the failure reason
     */
    public ErrorFieldDetails(String field, String issue) {
        this.field = field;
        this.issue = issue;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getIssue() {
        return issue;
    }

    public void setIssue(String issue) {
        this.issue = issue;
    }
}
