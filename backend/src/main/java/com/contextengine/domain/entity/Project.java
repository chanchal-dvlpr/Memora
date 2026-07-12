package com.contextengine.domain.entity;

import com.contextengine.domain.aggregate.AggregateRoot;
import com.contextengine.domain.valueobject.FeatureId;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.TaskId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Acts as the primary system entry point and lifecycle manager of the local development workspace.
 * Serves as the Aggregate Root for the Project aggregate and Project Management aggregate.
 */
public class Project implements AggregateRoot<ProjectId> {
    
    private final ProjectId id;
    private final Path rootDirectory;
    private final String title;
    private ProjectState state;
    private Workspace workspace;
    private final List<Module> modules;
    private final List<Feature> features;
    private final List<Decision> decisions;
    private final List<Task> tasks;
    private final List<Bug> bugs;
    private final List<Constraint> constraints;
    private final List<Assumption> assumptions;
    private final List<Dependency> dependencies;

    /**
     * Constructs a new Project in the INITIALIZING state.
     *
     * @param id the unique project ID
     * @param rootDirectory the absolute root directory path of the project
     * @param title the title of the project
     */
    public Project(ProjectId id, Path rootDirectory, String title) {
        this.id = Objects.requireNonNull(id, "ProjectId must not be null");
        this.rootDirectory = Objects.requireNonNull(rootDirectory, "Root directory must not be null");
        
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Project title must not be null or empty");
        }
        this.title = title.trim();
        this.state = ProjectState.INITIALIZING;
        this.modules = new ArrayList<>();
        this.features = new ArrayList<>();
        this.decisions = new ArrayList<>();
        this.tasks = new ArrayList<>();
        this.bugs = new ArrayList<>();
        this.constraints = new ArrayList<>();
        this.assumptions = new ArrayList<>();
        this.dependencies = new ArrayList<>();
    }

    @Override
    public ProjectId id() {
        return id;
    }

    public Path rootDirectory() {
        return rootDirectory;
    }

    public String title() {
        return title;
    }

    public ProjectState state() {
        return state;
    }

    public Workspace workspace() {
        return workspace;
    }

    /**
     * Binds the Workspace to this Project.
     *
     * @param workspace the workspace to bind
     */
    public void bindWorkspace(Workspace workspace) {
        Objects.requireNonNull(workspace, "Workspace must not be null");
        if (!workspace.projectId().equals(this.id)) {
            throw new IllegalArgumentException("Workspace ProjectId must match the parent ProjectId");
        }
        this.workspace = workspace;
    }

    public List<Module> modules() {
        return Collections.unmodifiableList(modules);
    }

    /**
     * Adds a Module to this Project.
     *
     * @param module the module to add
     */
    public void addModule(Module module) {
        Objects.requireNonNull(module, "Module must not be null");
        if (!module.projectId().equals(this.id)) {
            throw new IllegalArgumentException("Module ProjectId must match this ProjectId");
        }
        // Verify path uniqueness / non-overlap invariant
        for (Module existing : modules) {
            if (existing.relativePath().equals(module.relativePath())) {
                throw new IllegalArgumentException("A module with path '" + module.relativePath() + "' already exists");
            }
        }
        modules.add(module);
    }

    public List<Feature> features() {
        return Collections.unmodifiableList(features);
    }

    /**
     * Adds a Feature milestone to this Project.
     *
     * @param feature the feature to add
     */
    public void addFeature(Feature feature) {
        Objects.requireNonNull(feature, "Feature must not be null");
        if (!feature.projectId().equals(this.id)) {
            throw new IllegalArgumentException("Feature ProjectId must match this ProjectId");
        }
        features.add(feature);
    }

    public List<Decision> decisions() {
        return Collections.unmodifiableList(decisions);
    }

    /**
     * Adds a Decision to this Project.
     *
     * @param decision the decision to add
     */
    public void addDecision(Decision decision) {
        Objects.requireNonNull(decision, "Decision must not be null");
        if (!decision.projectId().equals(this.id)) {
            throw new IllegalArgumentException("Decision ProjectId must match this ProjectId");
        }
        decisions.add(decision);
    }

    public List<Task> tasks() {
        return Collections.unmodifiableList(tasks);
    }

    /**
     * Adds a Task to this Project.
     *
     * @param task the task to add
     */
    public void addTask(Task task) {
        Objects.requireNonNull(task, "Task must not be null");
        if (!task.projectId().equals(this.id)) {
            throw new IllegalArgumentException("Task ProjectId must match this ProjectId");
        }
        tasks.add(task);
        if (task.featureId() != null) {
            recalculateFeatureProgress(task.featureId());
        }
    }

    /**
     * Starts execution of a task.
     *
     * @param taskId the ID of the task to start
     */
    public void startTaskExecution(TaskId taskId) {
        Task task = tasks.stream()
            .filter(t -> t.id().equals(taskId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Task not found in project"));
        
        task.startExecution();
    }

    /**
     * Completes a task and triggers dynamic feature progress update.
     * Enforces the consistency rule: "Completing a Task must update the progress state of the parent Feature."
     *
     * @param taskId the ID of the task to complete
     */
    public void completeTask(TaskId taskId) {
        Task task = tasks.stream()
            .filter(t -> t.id().equals(taskId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Task not found in project"));
        
        task.complete();
        
        if (task.featureId() != null) {
            recalculateFeatureProgress(task.featureId());
        }
    }

    private void recalculateFeatureProgress(FeatureId featureId) {
        Feature feature = features.stream()
            .filter(f -> f.id().equals(featureId))
            .findFirst()
            .orElse(null);
        
        if (feature == null) {
            return;
        }
            
        long totalTasks = tasks.stream().filter(t -> featureId.equals(t.featureId())).count();
        if (totalTasks > 0) {
            long completedTasks = tasks.stream()
                .filter(t -> featureId.equals(t.featureId()) && t.status() == TaskState.COMPLETED)
                .count();
            double progress = (double) completedTasks / totalTasks * 100.0;
            feature.updateProgress(progress);
        }
    }

    public List<Bug> bugs() {
        return Collections.unmodifiableList(bugs);
    }

    public void addBug(Bug bug) {
        Objects.requireNonNull(bug, "Bug must not be null");
        if (!bug.projectId().equals(this.id)) {
            throw new IllegalArgumentException("Bug ProjectId must match this ProjectId");
        }
        bugs.add(bug);
    }

    public List<Constraint> constraints() {
        return Collections.unmodifiableList(constraints);
    }

    public void addConstraint(Constraint constraint) {
        Objects.requireNonNull(constraint, "Constraint must not be null");
        if (!constraint.projectId().equals(this.id)) {
            throw new IllegalArgumentException("Constraint ProjectId must match this ProjectId");
        }
        constraints.add(constraint);
    }

    public List<Assumption> assumptions() {
        return Collections.unmodifiableList(assumptions);
    }

    public void addAssumption(Assumption assumption) {
        Objects.requireNonNull(assumption, "Assumption must not be null");
        if (!assumption.projectId().equals(this.id)) {
            throw new IllegalArgumentException("Assumption ProjectId must match this ProjectId");
        }
        assumptions.add(assumption);
    }

    public List<Dependency> dependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    public void addDependency(Dependency dependency) {
        Objects.requireNonNull(dependency, "Dependency must not be null");
        if (!dependency.projectId().equals(this.id)) {
            throw new IllegalArgumentException("Dependency ProjectId must match this ProjectId");
        }
        dependencies.add(dependency);
    }

    /**
     * Transitions the project state to ACTIVE upon successful initial scan completion.
     */
    public void activate() {
        if (this.state != ProjectState.INITIALIZING && this.state != ProjectState.IDLE && this.state != ProjectState.SUSPENDED) {
            throw new IllegalStateException("Cannot transition to ACTIVE state from " + this.state);
        }
        this.state = ProjectState.ACTIVE;
    }

    /**
     * Transitions the project state to IDLE upon system inactivity.
     */
    public void idle() {
        if (this.state != ProjectState.ACTIVE) {
            throw new IllegalStateException("Cannot transition to IDLE state from " + this.state);
        }
        this.state = ProjectState.IDLE;
    }

    /**
     * Transitions the project state to SUSPENDED due to resource saving or low battery.
     */
    public void suspend() {
        if (this.state == ProjectState.INITIALIZING || this.state == ProjectState.ARCHIVED) {
            throw new IllegalStateException("Cannot transition to SUSPENDED state from " + this.state);
        }
        this.state = ProjectState.SUSPENDED;
    }

    /**
     * Transitions the project state to ARCHIVED on unregistration request.
     */
    public void archive() {
        if (this.state == ProjectState.ARCHIVED) {
            throw new IllegalStateException("Project is already ARCHIVED");
        }
        this.state = ProjectState.ARCHIVED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Project project)) return false;
        return id.equals(project.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
