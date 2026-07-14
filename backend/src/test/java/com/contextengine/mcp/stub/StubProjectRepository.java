package com.contextengine.mcp.stub;

import com.contextengine.domain.entity.Project;
import com.contextengine.domain.repository.ProjectRepository;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class StubProjectRepository implements ProjectRepository {
    @Override
    public void save(Project project) {}

    @Override
    public Optional<Project> findById(ProjectId projectId) {
        return Optional.empty();
    }

    @Override
    public Optional<Project> findByPath(Path absolutePath) {
        return Optional.empty();
    }

    @Override
    public Collection<Project> findAllActive() {
        return Collections.emptyList();
    }

    @Override
    public void remove(ProjectId projectId) {}
}
