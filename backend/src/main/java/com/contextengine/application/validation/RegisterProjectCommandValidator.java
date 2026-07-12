package com.contextengine.application.validation;

import com.contextengine.application.command.RegisterProjectCommand;
import java.util.Objects;

/**
 * Validator enforcing non-null constraints and content limits on project registration commands.
 * <p>
 * Bounded Context: Project Management
 * Related Command: RegisterProjectCommand
 * </p>
 */
public class RegisterProjectCommandValidator implements RequestValidator<RegisterProjectCommand> {

    @Override
    public void validate(RegisterProjectCommand command) {
        Objects.requireNonNull(command, "RegisterProjectCommand must not be null");
        Objects.requireNonNull(command.absoluteRootPath(), "Absolute root path must not be null");
        Objects.requireNonNull(command.projectTitle(), "Project title must not be null");
        
        if (command.projectTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Project title must not be empty");
        }
        if (command.exclusions() == null) {
            throw new IllegalArgumentException("Exclusions list must not be null");
        }
    }
}
