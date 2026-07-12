package com.contextengine.infrastructure.ai;

/**
 * Computes token counts for text strings using target tokenizer schemas.
 * <p>
 * Bounded Context: Context Assembly
 * Related Subsystem: AI Integration Subsystem (AI-SUB)
 * </p>
 */
public class TokenCalculator {

    /**
     * Estimates the token count of a given string.
     *
     * @param text the text to estimate
     * @return token count
     */
    public int calculateTokens(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / 4.0);
    }
}
