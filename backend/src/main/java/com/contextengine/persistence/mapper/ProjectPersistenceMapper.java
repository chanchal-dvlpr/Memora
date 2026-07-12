package com.contextengine.persistence.mapper;

import com.contextengine.domain.entity.Project;
import com.contextengine.domain.entity.ProjectState;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.persistence.entity.ProjectEntity;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Aggregate Root Persistence mapper converting between Project domain model and ProjectEntity.
 * <p>
 * Bounded Context: Project Management
 * Related Subsystem: Persistent Storage
 * </p>
 */
public class ProjectPersistenceMapper {

    private final WorkspacePersistenceMapper workspaceMapper = new WorkspacePersistenceMapper();
    private final ModulePersistenceMapper moduleMapper = new ModulePersistenceMapper();
    private final FeaturePersistenceMapper featureMapper = new FeaturePersistenceMapper();
    private final DecisionPersistenceMapper decisionMapper = new DecisionPersistenceMapper();
    private final TaskPersistenceMapper taskMapper = new TaskPersistenceMapper();
    private final BugPersistenceMapper bugMapper = new BugPersistenceMapper();
    private final ConstraintPersistenceMapper constraintMapper = new ConstraintPersistenceMapper();
    private final AssumptionPersistenceMapper assumptionMapper = new AssumptionPersistenceMapper();
    private final DependencyPersistenceMapper dependencyMapper = new DependencyPersistenceMapper();

    /**
     * Converts Project domain model to ProjectEntity.
     *
     * @param project domain model
     * @return project entity
     */
    public ProjectEntity toEntity(Project project) {
        if (project == null) return null;
        ProjectEntity entity = new ProjectEntity();
        entity.setId(project.id().value().toString());
        entity.setRootPath(project.rootDirectory().value());
        entity.setTitle(project.title());
        entity.setState(project.state().name());

        if (project.workspace() != null) {
            entity.setWorkspace(workspaceMapper.toEntity(project.workspace(), entity));
        }

        entity.setModules(project.modules().stream()
            .map(m -> moduleMapper.toEntity(m, entity))
            .collect(Collectors.toList()));

        entity.setFeatures(project.features().stream()
            .map(f -> featureMapper.toEntity(f, entity))
            .collect(Collectors.toList()));

        entity.setDecisions(project.decisions().stream()
            .map(d -> decisionMapper.toEntity(d, entity))
            .collect(Collectors.toList()));

        entity.setTasks(project.tasks().stream()
            .map(t -> taskMapper.toEntity(t, entity))
            .collect(Collectors.toList()));

        entity.setBugs(project.bugs().stream()
            .map(b -> bugMapper.toEntity(b, entity))
            .collect(Collectors.toList()));

        entity.setConstraints(project.constraints().stream()
            .map(c -> constraintMapper.toEntity(c, entity))
            .collect(Collectors.toList()));

        entity.setAssumptions(project.assumptions().stream()
            .map(a -> assumptionMapper.toEntity(a, entity))
            .collect(Collectors.toList()));

        entity.setDependencies(project.dependencies().stream()
            .map(d -> dependencyMapper.toEntity(d, entity))
            .collect(Collectors.toList()));

        return entity;
    }

    /**
     * Converts ProjectEntity to Project domain model.
     *
     * @param entity project entity
     * @return project domain model
     */
    public Project toDomain(ProjectEntity entity) {
        if (entity == null) return null;
        ProjectId projectId = new ProjectId(UUID.fromString(entity.getId()));
        Path rootPath = new Path(entity.getRootPath());
        String title = entity.getTitle();
        ProjectState state = ProjectState.valueOf(entity.getState());

        return Project.reconstruct(
            projectId,
            rootPath,
            title,
            state,
            workspaceMapper.toDomain(entity.getWorkspace(), projectId),
            entity.getModules().stream().map(m -> moduleMapper.toDomain(m, projectId)).collect(Collectors.toList()),
            entity.getFeatures().stream().map(f -> featureMapper.toDomain(f, projectId)).collect(Collectors.toList()),
            entity.getDecisions().stream().map(d -> decisionMapper.toDomain(d, projectId)).collect(Collectors.toList()),
            entity.getTasks().stream().map(t -> taskMapper.toDomain(t, projectId)).collect(Collectors.toList()),
            entity.getBugs().stream().map(b -> bugMapper.toDomain(b, projectId)).collect(Collectors.toList()),
            entity.getConstraints().stream().map(c -> constraintMapper.toDomain(c, projectId)).collect(Collectors.toList()),
            entity.getAssumptions().stream().map(a -> assumptionMapper.toDomain(a, projectId)).collect(Collectors.toList()),
            entity.getDependencies().stream().map(d -> dependencyMapper.toDomain(d, projectId)).collect(Collectors.toList())
        );
    }
}
