package com.contextengine.infrastructure.ai;

import java.util.Objects;

/**
 * AI Provider Adapter coordinating model integrations and prompt transport.
 * <p>
 * Bounded Context: AI Ingestion
 * Related Subsystem: AI Integration Subsystem (AI-SUB)
 * </p>
 */
public class AIProviderAdapter implements AIClient {

    @Override
    public String generateResponse(String prompt) {
        Objects.requireNonNull(prompt, "Prompt must not be null");
        return "Simulated LLM response for: " + prompt.substring(0, Math.min(prompt.length(), 30)) + "...";
    }
}
