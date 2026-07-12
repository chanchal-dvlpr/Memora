package com.contextengine.application.command;

import com.contextengine.domain.service.FormatEnum;
import com.contextengine.domain.valueobject.NodeId;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.SearchQuery;
import com.contextengine.domain.valueobject.TokenBudget;
import java.util.Objects;

/**
 * Command requesting to assemble and format context payloads conforming to token bounds.
 * <p>
 * Related Use Case: Context Generation
 * Related Bounded Context / Aggregate: Context
 * </p>
 */
public record GenerateContextCommand(
    ProjectId projectId,
    SearchQuery query,
    NodeId focusNode,
    TokenBudget budget,
    FormatEnum format
) implements Command {
    
    public GenerateContextCommand {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(query, "SearchQuery must not be null");
        Objects.requireNonNull(focusNode, "FocusNode must not be null");
        Objects.requireNonNull(budget, "TokenBudget must not be null");
        Objects.requireNonNull(format, "FormatEnum must not be null");
    }
}
