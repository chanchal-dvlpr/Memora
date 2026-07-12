package com.contextengine.infrastructure.ai;

/**
 * Interface client abstraction representing a Large Language Model connection.
 * <p>
 * Bounded Context: AI Ingestion
 * Related Subsystem: AI Integration Subsystem (AI-SUB)
 * </p>
 */
public interface AIClient {

    /**
     * Sends a text prompt and returns the model response.
     *
     * @param prompt the prompt message
     * @return generated response text
     */
    String generateResponse(String prompt);
}
