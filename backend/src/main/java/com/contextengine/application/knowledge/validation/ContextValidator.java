package com.contextengine.application.knowledge.validation;

import com.contextengine.application.knowledge.exception.KnowledgeException;
import com.contextengine.domain.entity.ContextSnapshot;
import com.contextengine.domain.valueobject.EngineeringEvidence;
import com.contextengine.domain.valueobject.TokenBudget;
import java.util.List;
import java.util.Objects;

/**
 * Validates the consistency and structural bounds of compiled ContextSnapshots.
 * <p>
 * Bounded Context: Context Assembly
 * Architecture Reference: Domain Modeling Design Section 2.15.3 (Validation Rules) and Database Design Section 4.15.1
 * Responsibility: Ensures generated snapshots comply with token budgets, format definitions, and contain well-formed citation evidences.
 * Dependencies: {@link ContextSnapshot}, {@link TokenBudget}, {@link KnowledgeException}.
 * </p>
 */
public class ContextValidator {

    /**
     * Performs validation checks on a generated ContextSnapshot against the configured budget.
     *
     * @param snapshot the context snapshot to validate
     * @param budget the target token budget
     * @throws KnowledgeException if snapshot content violates constraints
     */
    public void validate(ContextSnapshot snapshot, TokenBudget budget) {
        Objects.requireNonNull(snapshot, "ContextSnapshot must not be null");
        Objects.requireNonNull(budget, "TokenBudget must not be null");

        // 1. Enforce token budget limits
        if (snapshot.summary().tokenFootprint() > budget.value()) {
            throw new KnowledgeException(
                "Context snapshot footprint (" + snapshot.summary().tokenFootprint() +
                ") exceeds the allowed token budget (" + budget.value() + ")",
                "ERR_BUDGET_EXCEEDED"
            );
        }

        // 2. Validate key metrics are non-negative
        if (snapshot.summary().totalFileCount() < 0) {
            throw new KnowledgeException("Total file count cannot be negative", "ERR_INVALID_METRICS");
        }
        if (snapshot.summary().tokenFootprint() < 0) {
            throw new KnowledgeException("Token footprint cannot be negative", "ERR_INVALID_METRICS");
        }

        // 3. Validate engineering evidence ranges
        List<EngineeringEvidence> evidences = snapshot.evidences();
        if (evidences != null) {
            for (EngineeringEvidence evidence : evidences) {
                if (evidence.filePath() == null || evidence.filePath().value().isBlank()) {
                    throw new KnowledgeException("Evidence file path must not be empty", "ERR_INVALID_EVIDENCE");
                }
                if (evidence.startLine() <= 0) {
                    throw new KnowledgeException("Evidence start line must be positive", "ERR_INVALID_EVIDENCE");
                }
                if (evidence.endLine() < evidence.startLine()) {
                    throw new KnowledgeException("Evidence end line cannot be less than start line", "ERR_INVALID_EVIDENCE");
                }
                if (evidence.fileContentHash() == null || evidence.fileContentHash().value().isBlank()) {
                    throw new KnowledgeException("Evidence content hash must not be empty", "ERR_INVALID_EVIDENCE");
                }
            }
        }

        // 4. Validate citation consistency
        List<String> primaryEntities = snapshot.summary().primaryEntities();
        if (primaryEntities != null) {
            for (String entityUrn : primaryEntities) {
                if (entityUrn == null || !entityUrn.startsWith("urn:")) {
                    throw new KnowledgeException("Citation entity URN must start with 'urn:' prefix", "ERR_INVALID_CITATION");
                }
            }
        }
    }
}
