package com.contextengine.domain.validation;

import com.contextengine.domain.entity.Project;
import java.io.File;
import java.util.Collection;
import java.util.Objects;

/**
 * Validates project folder paths and nesting boundaries to protect configuration settings.
 */
public class ProjectValidator implements Validator<Project> {
    
    private final Collection<Project> existingProjects;

    /**
     * Constructs a ProjectValidator.
     *
     * @param existingProjects currently registered projects to check for nesting overlaps
     */
    public ProjectValidator(Collection<Project> existingProjects) {
        this.existingProjects = Objects.requireNonNull(existingProjects, "Existing projects list must not be null");
    }

    @Override
    public void validate(Project project) throws ValidationException {
        if (project == null) {
            throw new ValidationException("Project to validate must not be null");
        }
        
        File file = new File(project.rootDirectory().value());
        if (!file.exists() || !file.isDirectory()) {
            throw new ValidationException("Project root directory does not exist or is not a folder: " + project.rootDirectory().value());
        }
        if (!file.canWrite()) {
            throw new ValidationException("Project root directory lacks write access permissions: " + project.rootDirectory().value());
        }
        
        String pathStr = project.rootDirectory().value();
        for (Project existing : existingProjects) {
            if (existing.id().equals(project.id())) {
                continue;
            }
            String existingStr = existing.rootDirectory().value();
            if (pathStr.startsWith(existingStr) || existingStr.startsWith(pathStr)) {
                throw new ValidationException("Overlapping project directory detected with project: " + existing.title());
            }
        }
    }
}
