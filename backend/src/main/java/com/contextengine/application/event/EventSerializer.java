package com.contextengine.application.event;

import com.contextengine.domain.event.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to serialize Domain Event attributes into plain Map key-value entries.
 * Avoids Java reflection entirely in compliance with the architecture rules.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Event Subsystem (EV-SUB)
 * </p>
 */
public class EventSerializer {

    /**
     * Serializes a DomainEvent into a flat map of attributes.
     *
     * @param event domain event
     * @return map payload representation
     */
    public Map<String, Object> serialize(DomainEvent event) {
        if (event == null) {
            return Map.of();
        }

        Map<String, Object> payload = new HashMap<>();

        if (event instanceof ProjectRegistered ev) {
            payload.put("projectId", ev.projectId().value().toString());
            payload.put("absoluteRootPath", ev.absoluteRootPath().value());
        } else if (event instanceof ProjectScanned ev) {
            payload.put("projectId", ev.projectId().value().toString());
            payload.put("filesScannedCount", ev.filesScannedCount());
            payload.put("symbolsCount", ev.symbolsCount());
        } else if (event instanceof ScanStarted ev) {
            payload.put("projectId", ev.projectId().value().toString());
            payload.put("scanMode", ev.scanMode());
        } else if (event instanceof ScanCompleted ev) {
            payload.put("projectId", ev.projectId().value().toString());
            payload.put("scanMode", ev.scanMode());
            payload.put("filesScannedCount", ev.filesScannedCount());
            payload.put("symbolsCount", ev.symbolsCount());
        } else if (event instanceof ModuleDiscovered ev) {
            payload.put("moduleId", ev.moduleId().value().toString());
            payload.put("projectId", ev.projectId().value().toString());
            payload.put("moduleName", ev.moduleName());
            payload.put("manifestPath", ev.manifestPath().value());
        } else if (event instanceof FeatureCreated ev) {
            payload.put("featureId", ev.featureId().value().toString());
            payload.put("projectId", ev.projectId().value().toString());
            payload.put("title", ev.title());
            payload.put("priority", ev.priority().name());
        } else if (event instanceof FeatureUpdated ev) {
            payload.put("featureId", ev.featureId().value().toString());
            payload.put("projectId", ev.projectId().value().toString());
            payload.put("progressPercentage", ev.progressPercentage());
            payload.put("status", ev.status().name());
        } else if (event instanceof TaskCreated ev) {
            payload.put("taskId", ev.taskId().value().toString());
            if (ev.featureId() != null) {
                payload.put("featureId", ev.featureId().value().toString());
            }
            payload.put("projectId", ev.projectId().value().toString());
            payload.put("taskPriority", ev.taskPriority().name());
        } else if (event instanceof TaskCompleted ev) {
            payload.put("taskId", ev.taskId().value().toString());
            if (ev.featureId() != null) {
                payload.put("featureId", ev.featureId().value().toString());
            }
            payload.put("projectId", ev.projectId().value().toString());
        } else if (event instanceof DecisionRecorded ev) {
            payload.put("decisionId", ev.decisionId().value().toString());
            payload.put("projectId", ev.projectId().value().toString());
            payload.put("filePath", ev.filePath().value());
            payload.put("status", ev.status().name());
        } else if (event instanceof DecisionApproved ev) {
            payload.put("decisionId", ev.decisionId().value().toString());
            payload.put("projectId", ev.projectId().value().toString());
            payload.put("markdownPath", ev.markdownPath().value());
            payload.put("approvedBy", ev.approvedBy());
        } else if (event instanceof BugDetected ev) {
            payload.put("bugId", ev.bugId().value().toString());
            payload.put("projectId", ev.projectId().value().toString());
            if (ev.sourcePath() != null) {
                payload.put("sourcePath", ev.sourcePath().value());
            }
            if (ev.commitHash() != null) {
                payload.put("commitHash", ev.commitHash());
            }
        } else if (event instanceof ConstraintAdded ev) {
            payload.put("constraintId", ev.constraintId().value().toString());
            payload.put("projectId", ev.projectId().value().toString());
            payload.put("constraintType", ev.constraintType());
        } else if (event instanceof AssumptionVerified ev) {
            payload.put("assumptionId", ev.assumptionId().value().toString());
            payload.put("projectId", ev.projectId().value().toString());
            payload.put("verificationStatus", ev.verificationStatus().name());
        } else if (event instanceof DependencyUpdated ev) {
            payload.put("dependencyId", ev.dependencyId().value().toString());
            payload.put("projectId", ev.projectId().value().toString());
            payload.put("packageName", ev.packageName());
            payload.put("oldVersion", ev.oldVersion().value());
            payload.put("newVersion", ev.newVersion().value());
        } else if (event instanceof ContextGenerated ev) {
            payload.put("snapshotId", ev.snapshotId().value().toString());
            payload.put("projectId", ev.projectId().value().toString());
            payload.put("tokenCount", ev.tokenCount());
            payload.put("outputFormat", ev.outputFormat().name());
        } else if (event instanceof ContextRetrieved ev) {
            payload.put("projectId", ev.projectId().value().toString());
            payload.put("queryText", ev.queryText());
            payload.put("retrievedNodesCount", ev.retrievedNodesCount());
        } else if (event instanceof ContextSnapshotCreated ev) {
            payload.put("snapshotId", ev.snapshotId().value().toString());
            payload.put("projectId", ev.projectId().value().toString());
            payload.put("graphVersion", ev.graphVersion().value());
        } else if (event instanceof ContextVersionCreated ev) {
            payload.put("snapshotId", ev.snapshotId().value().toString());
            payload.put("version", ev.version().value());
            payload.put("deltaHash", ev.deltaHash().value());
        } else if (event instanceof SearchExecuted ev) {
            payload.put("queryText", ev.queryText());
            payload.put("searchType", ev.searchType());
            payload.put("searchTimeMs", ev.searchTimeMs());
            payload.put("resultsCount", ev.resultsCount());
        } else if (event instanceof KnowledgeGraphUpdated ev) {
            payload.put("projectId", ev.projectId().value().toString());
            payload.put("nodesAddedCount", ev.nodesAddedCount());
            payload.put("edgesAddedCount", ev.edgesAddedCount());
        } else if (event instanceof AIHandoffGenerated ev) {
            payload.put("projectId", ev.projectId().value().toString());
            payload.put("handoffPath", ev.handoffPath().value());
            payload.put("tokenCount", ev.tokenCount());
        }

        return payload;
    }
}
