package com.contextengine.test.knowledge;

import com.contextengine.application.knowledge.context.ContextValidator;
import com.contextengine.application.knowledge.budget.BudgetConfiguration;
import com.contextengine.application.knowledge.budget.BudgetValidator;
import com.contextengine.test.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Failure and recovery testing evaluating STRICT/LAX boundaries and invalid configs.
 */
class KnowledgeEngineFailureRecoveryTest extends BaseIntegrationTest {

    @Test
    void testInvalidSubsystemConfigurations() {
        BudgetValidator budgetValidator = new BudgetValidator();
        // Null verification
        var res = budgetValidator.validate(null, null);
        assertFalse(res.isValid());
        assertTrue(res.errors().stream().anyMatch(e -> e.contains("BudgetConfiguration is null")));
    }

    @Test
    void testMalformedGraphValidation() {
        ContextValidator contextValidator = new ContextValidator();
        assertThrows(NullPointerException.class, () -> {
            contextValidator.validate(null, null);
        });
    }
}
