package com.contextengine.domain.entity;

/**
 * Represents the authority and integration states of a design choice or Decision (ADR).
 */
public enum DecisionState {
    /** Markdown ADR drafted; design record registered. */
    PROPOSED,
    
    /** Design record validated and approved by the team. */
    APPROVED,
    
    /** Design choice rejected; context preserved in history logs. */
    REJECTED,
    
    /** Bounded implementation replaced by a newer decision. */
    SUPERSEDED
}
