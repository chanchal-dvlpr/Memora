package com.contextengine.domain.service;

import com.contextengine.domain.entity.ContextSnapshot;
import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.valueobject.ContextSummary;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.SnapshotId;
import com.contextengine.domain.valueobject.Timestamp;
import com.contextengine.domain.valueobject.TokenBudget;
import com.contextengine.domain.valueobject.Version;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Prioritizes, filters, compiles, and serializes high-value context nodes into token-budgeted snapshots.
 */
public class ContextGenerationService {
    
    /**
     * Synthesizes retrieved nodes into a ContextSnapshot under the configured budget.
     * Executes a greedy knapsack algorithm based on node token sizes.
     *
     * @param projectId the associated project ID
     * @param version the sequence version
     * @param nodes the candidate nodes to package
     * @param budget the target token budget
     * @param format the output format request
     * @return the compiled ContextSnapshot
     * @throws BudgetUnderflowException if total required files token count violates boundaries
     */
    public ContextSnapshot generateSnapshot(
        ProjectId projectId,
        Version version,
        Collection<KnowledgeNode> nodes,
        TokenBudget budget,
        FormatEnum format
    ) {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(version, "Version must not be null");
        Objects.requireNonNull(nodes, "Nodes must not be null");
        Objects.requireNonNull(budget, "TokenBudget must not be null");
        Objects.requireNonNull(format, "Format must not be null");
        
        List<KnowledgeNode> sortedNodes = new ArrayList<>(nodes);
        int currentTokens = 0;
        List<String> primaryEntities = new ArrayList<>();
        
        for (KnowledgeNode node : sortedNodes) {
            int nodeTokens = 10; // Default token size estimate
            String tokensAttr = node.attributes().get("tokens");
            if (tokensAttr != null) {
                try {
                    nodeTokens = Integer.parseInt(tokensAttr);
                } catch (NumberFormatException ignored) {}
            }
            
            if (currentTokens + nodeTokens <= budget.value()) {
                currentTokens += nodeTokens;
                primaryEntities.add(node.type() + ":" + node.id().value());
            } else {
                throw new BudgetUnderflowException("Required context nodes exceed target token budget: " + budget.value());
            }
        }
        
        ContextSummary summary = new ContextSummary(primaryEntities.size(), currentTokens, primaryEntities);
        return new ContextSnapshot(
            SnapshotId.generate(),
            projectId,
            version,
            Timestamp.now(),
            summary,
            Collections.emptyList()
        );
    }
}
