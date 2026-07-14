package com.contextengine.application.knowledge.budget;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates that budgeted context results do not exceed limits or violate structural rules.
 */
public class BudgetValidator {

    /**
     * Validates a list of budgeted fragments.
     *
     * @param budgetedFragments list of budgeted fragments
     * @param config            budget limits configuration
     * @return validation result holding validation status and errors
     */
    public BudgetValidationResult validate(List<BudgetedFragment> budgetedFragments, BudgetConfiguration config) {
        List<String> errors = new ArrayList<>();

        if (config == null) {
            errors.add("BudgetConfiguration is null");
            return new BudgetValidationResult(false, errors);
        }

        if (budgetedFragments == null) {
            errors.add("budgetedFragments list is null");
            return new BudgetValidationResult(false, errors);
        }

        Set<String> fragmentIds = new HashSet<>();
        int includedCount = 0;
        int reservedCount = 0;
        int excludedCount = 0;
        int totalChars = 0;
        int totalTokens = 0;

        for (int i = 0; i < budgetedFragments.size(); i++) {
            BudgetedFragment bf = budgetedFragments.get(i);
            if (bf == null) {
                errors.add("BudgetedFragment at index " + i + " is null");
                continue;
            }

            if (bf.rankedFragment() == null || bf.rankedFragment().fragment() == null) {
                errors.add("RankedFragment at index " + i + " is null");
                continue;
            }

            String id = bf.rankedFragment().fragment().fragmentId();
            if (!fragmentIds.add(id)) {
                errors.add("Duplicate BudgetedFragment ID: " + id);
            }

            BudgetDecision decision = bf.decision();
            BudgetReason reason = bf.reason();

            if (decision == null) {
                errors.add("BudgetDecision at index " + i + " is null");
                continue;
            }
            if (reason == null) {
                errors.add("BudgetReason at index " + i + " is null");
                continue;
            }

            int charLength = bf.rankedFragment().fragment().content() != null
                ? bf.rankedFragment().fragment().content().length() : 0;
            int estimatedTokens = Math.max(1, charLength / 4);

            // Validate reservations
            if (decision == BudgetDecision.RESERVED && reason != BudgetReason.RESERVED_SYSTEM_CONTEXT) {
                errors.add("Reserved fragment at index " + i + " has invalid reason: " + reason.name());
            }
            if (reason == BudgetReason.RESERVED_SYSTEM_CONTEXT && decision != BudgetDecision.RESERVED) {
                errors.add("Fragment at index " + i + " has RESERVED_SYSTEM_CONTEXT reason but decision is: " + decision.name());
            }

            if (decision == BudgetDecision.INCLUDED || decision == BudgetDecision.RESERVED) {
                if (decision == BudgetDecision.INCLUDED) {
                    includedCount++;
                } else {
                    reservedCount++;
                }
                totalChars += charLength;
                totalTokens += estimatedTokens;
            } else {
                excludedCount++;
            }
        }

        // Limit validations
        if (includedCount > config.maximumFragments()) {
            errors.add("Total included fragments count " + includedCount + " exceeds maximum limit: " + config.maximumFragments());
        }

        if (totalChars > config.maximumCharacters()) {
            errors.add("Total allocated characters " + totalChars + " exceeds maximum limit: " + config.maximumCharacters());
        }

        if (totalTokens > config.maximumTokens()) {
            errors.add("Total allocated tokens " + totalTokens + " exceeds maximum limit: " + config.maximumTokens());
        }

        return new BudgetValidationResult(errors.isEmpty(), errors);
    }
}
