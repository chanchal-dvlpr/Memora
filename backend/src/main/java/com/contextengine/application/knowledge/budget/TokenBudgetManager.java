package com.contextengine.application.knowledge.budget;

import com.contextengine.application.knowledge.ranking.ContextRankedResult;
import com.contextengine.domain.event.ContextBudgetExceeded;
import com.contextengine.domain.event.DomainEventPublisher;
import com.contextengine.domain.valueobject.Metadata;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.SnapshotId;
import com.contextengine.domain.valueobject.TokenBudget;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service enforcing configured context window token constraints.
 * <p>
 * Bounded Context: Context Assembly
 * Architecture Reference: Functional Requirement FR-014 (Context Generation Engine) Section 12 (Token Budget Optimization)
 * Responsibility: Executes progressive pruning passes and knapsack dynamic programming optimizations to fit context payloads.
 * Dependencies: {@link DomainEventPublisher}, {@link ContextRankedResult}, {@link TokenBudget}.
 * </p>
 */
public class TokenBudgetManager {

    private final DomainEventPublisher eventPublisher;

    /**
     * Constructs a TokenBudgetManager.
     *
     * @param eventPublisher publisher for domain events
     */
    public TokenBudgetManager(DomainEventPublisher eventPublisher) {
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "DomainEventPublisher must not be null");
    }

    /**
     * Applies budgeting limits and returns a subset of candidates that fit within the token budget.
     *
     * @param candidates initial collection of ranked candidates
     * @param budget the target token budget
     * @param projectId parent project ID
     * @param contextId active session/snapshot ID
     * @return list of budgeted ranked results
     */
    public List<ContextRankedResult> applyBudget(
        List<ContextRankedResult> candidates,
        TokenBudget budget,
        ProjectId projectId,
        SnapshotId contextId
    ) {
        Objects.requireNonNull(candidates, "Candidates list must not be null");
        Objects.requireNonNull(budget, "TokenBudget must not be null");
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(contextId, "SnapshotId contextId must not be null");

        int maxBudget = budget.value();
        if (maxBudget <= 0 || candidates.isEmpty()) {
            return new ArrayList<>();
        }

        // Calculate initial footprint
        int totalTokens = calculateTotalTokens(candidates);
        if (totalTokens <= maxBudget) {
            return new ArrayList<>(candidates);
        }

        List<Metadata> pruningActions = new ArrayList<>();

        // PASS 1: Drop COMMITS and TASKS
        List<ContextRankedResult> current = new ArrayList<>(candidates);
        List<ContextRankedResult> pass1 = current.stream()
            .filter(c -> !c.node().type().equalsIgnoreCase("COMMIT") && !c.node().type().equalsIgnoreCase("TASK"))
            .collect(Collectors.toList());

        int removedInPass1 = current.size() - pass1.size();
        if (removedInPass1 > 0) {
            int savedTokens = totalTokens - calculateTotalTokens(pass1);
            pruningActions.add(new Metadata(Map.of(
                "element_type", "GIT_LOG_OR_TASKS",
                "identifier", "pass-1-pruning",
                "tokens_saved", String.valueOf(savedTokens)
            )));
        }

        current = pass1;
        totalTokens = calculateTotalTokens(current);
        if (totalTokens <= maxBudget) {
            publishBudgetExceededEvent(contextId, projectId, calculateTotalTokens(candidates), maxBudget, pruningActions);
            return current;
        }

        // PASS 2: Stub dependencies (simulate signature-only by reducing token counts to 5)
        List<ContextRankedResult> pass2 = new ArrayList<>();
        int savedTokensInPass2 = 0;
        for (ContextRankedResult candidate : current) {
            if (candidate.node().type().equalsIgnoreCase("DEPENDENCY")) {
                // Create a stubbed node with reduced tokens attribute
                Map<String, String> newAttrs = new HashMap<>(candidate.node().attributes().values());
                int originalTokens = getTokens(candidate.node());
                newAttrs.put("tokens", "5");
                savedTokensInPass2 += (originalTokens - 5);

                com.contextengine.domain.entity.KnowledgeNode stubbedNode = new com.contextengine.domain.entity.KnowledgeNode(
                    candidate.node().id(),
                    candidate.node().type(),
                    new Metadata(newAttrs)
                );
                pass2.add(new ContextRankedResult(stubbedNode, candidate.score()));
            } else {
                pass2.add(candidate);
            }
        }

        if (savedTokensInPass2 > 0) {
            pruningActions.add(new Metadata(Map.of(
                "element_type", "DEPENDENCY",
                "identifier", "pass-2-stubbing",
                "tokens_saved", String.valueOf(savedTokensInPass2)
            )));
        }

        current = pass2;
        totalTokens = calculateTotalTokens(current);
        if (totalTokens <= maxBudget) {
            publishBudgetExceededEvent(contextId, projectId, calculateTotalTokens(candidates), maxBudget, pruningActions);
            return current;
        }

        // PASS 3: Prune adjacent nodes with distance >= 2 (keep active file at distance 0 and direct elements at distance 1)
        // BFS distances are stored inside "distance" attribute, default 1 if not present.
        List<ContextRankedResult> pass3 = current.stream()
            .filter(c -> {
                String distStr = c.node().attributes().get("distance");
                if (distStr != null) {
                    try {
                        int dist = Integer.parseInt(distStr);
                        return dist < 2;
                    } catch (NumberFormatException ignored) {}
                }
                return true;
            })
            .collect(Collectors.toList());

        int removedInPass3 = current.size() - pass3.size();
        if (removedInPass3 > 0) {
            int savedTokens = totalTokens - calculateTotalTokens(pass3);
            pruningActions.add(new Metadata(Map.of(
                "element_type", "INDIRECT_NODES",
                "identifier", "pass-3-hop-pruning",
                "tokens_saved", String.valueOf(savedTokens)
            )));
        }

        current = pass3;
        totalTokens = calculateTotalTokens(current);
        if (totalTokens <= maxBudget) {
            publishBudgetExceededEvent(contextId, projectId, calculateTotalTokens(candidates), maxBudget, pruningActions);
            return current;
        }

        // PASS 4: 0-1 Knapsack Optimization Fallback
        List<ContextRankedResult> pass4 = optimizeKnapsack(current, maxBudget);
        int finalSaved = totalTokens - calculateTotalTokens(pass4);
        if (finalSaved > 0) {
            pruningActions.add(new Metadata(Map.of(
                "element_type", "KNAPSACK_SELECTION",
                "identifier", "pass-4-knapsack",
                "tokens_saved", String.valueOf(finalSaved)
            )));
        }

        publishBudgetExceededEvent(contextId, projectId, calculateTotalTokens(candidates), maxBudget, pruningActions);
        return pass4;
    }

    private int calculateTotalTokens(List<ContextRankedResult> list) {
        return list.stream().mapToInt(c -> getTokens(c.node())).sum();
    }

    private int getTokens(com.contextengine.domain.entity.KnowledgeNode node) {
        String tokensStr = node.attributes().get("tokens");
        if (tokensStr != null) {
            try {
                return Integer.parseInt(tokensStr);
            } catch (NumberFormatException ignored) {}
        }
        return 10; // Default token size estimate
    }

    private List<ContextRankedResult> optimizeKnapsack(List<ContextRankedResult> items, int capacity) {
        int n = items.size();
        int[] dp = new int[capacity + 1];
        boolean[][] keep = new boolean[n][capacity + 1];

        for (int i = 0; i < n; i++) {
            ContextRankedResult item = items.get(i);
            int weight = getTokens(item.node());
            int value = (int) (item.score() * 1000);

            for (int w = capacity; w >= weight; w--) {
                int newValue = dp[w - weight] + value;
                if (newValue > dp[w]) {
                    dp[w] = newValue;
                    keep[i][w] = true;
                }
            }
        }

        List<ContextRankedResult> selected = new ArrayList<>();
        int w = capacity;
        for (int i = n - 1; i >= 0; i--) {
            if (keep[i][w]) {
                ContextRankedResult selectedItem = items.get(i);
                selected.add(selectedItem);
                w -= getTokens(selectedItem.node());
            }
        }

        // Maintain original deterministic ordering of items
        Collections.reverse(selected);
        return selected;
    }

    private void publishBudgetExceededEvent(
        SnapshotId contextId,
        ProjectId projectId,
        int requested,
        int budgetLimit,
        List<Metadata> actions
    ) {
        ContextBudgetExceeded event = new ContextBudgetExceeded(
            contextId,
            projectId,
            requested,
            budgetLimit,
            actions,
            Instant.now()
        );
        eventPublisher.publish(event);
    }
}
