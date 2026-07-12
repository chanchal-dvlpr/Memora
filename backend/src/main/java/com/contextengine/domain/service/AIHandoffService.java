package com.contextengine.domain.service;

import com.contextengine.domain.entity.Decision;
import com.contextengine.domain.entity.Feature;
import com.contextengine.domain.entity.Project;
import com.contextengine.domain.entity.Task;
import java.util.Objects;

/**
 * Aggregates, structures, and exports active project tracking variables for downstream AI consumption.
 */
public class AIHandoffService {
    
    /**
     * Generates a formatted markdown handoff package from the Project aggregate.
     * Enforces that citation markers link back to code files.
     *
     * @param project the project aggregate root to package
     * @return the formatted markdown string
     * @throws HandoffGenerationException if project data is corrupted or inaccessible
     * @throws NullPointerException if project is null
     */
    public String generateHandoffDocument(Project project) {
        Objects.requireNonNull(project, "Project must not be null");
        
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("# AI Handoff Document - ").append(project.title()).append("\n\n");
            sb.append("## Project Root: ").append(project.rootDirectory().value()).append("\n");
            sb.append("## Status: ").append(project.state()).append("\n\n");
            
            sb.append("### Active Features:\n");
            for (Feature feature : project.features()) {
                sb.append("- **").append(feature.title()).append("**: ")
                  .append(feature.status()).append(" (")
                  .append(feature.progressPercentage()).append("%)\n");
            }
            
            sb.append("\n### Tasks:\n");
            for (Task task : project.tasks()) {
                sb.append("- ").append(task.description()).append(" (")
                  .append(task.status()).append(")\n");
            }
            
            sb.append("\n### Decisions (ADRs):\n");
            for (Decision decision : project.decisions()) {
                sb.append("- [").append(decision.title()).append("](")
                  .append(decision.markdownPath().value()).append(") (")
                  .append(decision.status()).append(")\n");
            }
            
            return sb.toString();
        } catch (Exception e) {
            throw new HandoffGenerationException("Failed to generate handoff document: " + e.getMessage());
        }
    }
}
