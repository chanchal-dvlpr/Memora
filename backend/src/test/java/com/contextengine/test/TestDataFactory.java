package com.contextengine.test;

import com.contextengine.domain.entity.*;
import com.contextengine.domain.valueobject.*;
import java.util.Collections;
import java.util.List;

/**
 * Reusable test data factory for creating domain entities and value objects.
 */
public class TestDataFactory {

    public static ProjectId createProjectId() {
        return ProjectId.generate();
    }

    public static Path createPath(String value) {
        return new Path(value);
    }

    public static Path createDefaultPath() {
        return new Path("src/main/java");
    }

    public static Project createProject(ProjectId projectId, String title) {
        return new Project(projectId, createDefaultPath(), title);
    }

    public static Project createProject(String title) {
        return createProject(createProjectId(), title);
    }

    public static Project createDefaultProject() {
        return createProject("Default Project");
    }

    public static TokenBudget createTokenBudget(int limit) {
        return new TokenBudget(limit);
    }

    public static TokenBudget createDefaultTokenBudget() {
        return createTokenBudget(10000);
    }

    public static Context createContext(ProjectId projectId) {
        return new Context(projectId, createDefaultTokenBudget());
    }

    public static Context createDefaultContext() {
        return createContext(createProjectId());
    }

    public static FeatureId createFeatureId() {
        return FeatureId.generate();
    }

    public static Feature createFeature(FeatureId id, ProjectId projectId, String title, Priority priority) {
        return new Feature(id, projectId, title, priority);
    }

    public static Feature createFeature(ProjectId projectId, String title) {
        return createFeature(createFeatureId(), projectId, title, Priority.HIGH);
    }

    public static Feature createDefaultFeature() {
        return createFeature(createProjectId(), "Default Feature");
    }

    public static TaskId createTaskId() {
        return TaskId.generate();
    }

    public static Task createTask(TaskId id, FeatureId featureId, ProjectId projectId, String description, Priority priority, List<TaskId> dependencyTaskIds) {
        return new Task(id, featureId, projectId, description, priority, dependencyTaskIds);
    }

    public static Task createTask(ProjectId projectId, FeatureId featureId, String description) {
        return createTask(createTaskId(), featureId, projectId, description, Priority.HIGH, Collections.emptyList());
    }

    public static Task createDefaultTask() {
        return createTask(createProjectId(), createFeatureId(), "Default Task Description");
    }

    public static DecisionId createDecisionId() {
        return DecisionId.generate();
    }

    public static Decision createDecision(DecisionId id, ProjectId projectId, String title, Path markdownPath) {
        return new Decision(id, projectId, title, markdownPath);
    }

    public static Decision createDecision(ProjectId projectId, String title) {
        return createDecision(createDecisionId(), projectId, title, createDefaultPath());
    }

    public static Decision createDefaultDecision() {
        return createDecision(createProjectId(), "Default Decision Title");
    }
}
