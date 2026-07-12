package com.contextengine.persistence.repository;

import com.contextengine.domain.entity.Project;
import com.contextengine.domain.repository.ProjectRepository;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.persistence.entity.ProjectEntity;
import com.contextengine.persistence.mapper.ProjectPersistenceMapper;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Technical persistence implementation of the ProjectRepository using Spring Data JPA.
 * Delegates mapping to {@link ProjectPersistenceMapper}.
 * <p>
 * Bounded Context: Project Management
 * Related Subsystem: Persistent Storage
 * </p>
 */
public class ProjectRepositoryImpl implements ProjectRepository {

    private final SpringDataProjectRepository springDataRepository;
    private final ProjectPersistenceMapper mapper = new ProjectPersistenceMapper();

    /**
     * Constructs a ProjectRepositoryImpl.
     *
     * @param springDataRepository the spring data JPA repository
     */
    public ProjectRepositoryImpl(SpringDataProjectRepository springDataRepository) {
        this.springDataRepository = Objects.requireNonNull(springDataRepository, "SpringDataProjectRepository must not be null");
    }

    @Override
    public void save(Project project) {
        Objects.requireNonNull(project, "Project must not be null");
        ProjectEntity entity = mapper.toEntity(project);
        springDataRepository.save(entity);
    }

    @Override
    public Optional<Project> findById(ProjectId projectId) {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        return springDataRepository.findById(projectId.value().toString())
            .map(mapper::toDomain);
    }

    @Override
    public Optional<Project> findByPath(Path absolutePath) {
        Objects.requireNonNull(absolutePath, "Path must not be null");
        return springDataRepository.findByRootPath(absolutePath.value())
            .map(mapper::toDomain);
    }

    @Override
    public Collection<Project> findAllActive() {
        return springDataRepository.findAllActive().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public void remove(ProjectId projectId) {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        springDataRepository.deleteById(projectId.value().toString());
    }
}
