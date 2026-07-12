package com.contextengine.application.validation;

import com.contextengine.application.command.GenerateContextCommand;
import java.util.Objects;

/**
 * Validator enforcing budget constraints and content limits on context generation commands.
 * <p>
 * Bounded Context: Context Assembly
 * Related Command: GenerateContextCommand
 * </p>
 */
public class GenerateContextCommandValidator implements RequestValidator<GenerateContextCommand> {

    @Override
    public void validate(GenerateContextCommand command) {
        Objects.requireNonNull(command, "GenerateContextCommand must not be null");
        Objects.requireNonNull(command.projectId(), "ProjectId must not be null");
        Objects.requireNonNull(command.query(), "SearchQuery must not be null");
        Objects.requireNonNull(command.focusNode(), "FocusNode must not be null");
        Objects.requireNonNull(command.budget(), "TokenBudget must not be null");
        Objects.requireNonNull(command.format(), "FormatEnum must not be null");

        if (command.budget().value() <= 0) {
            throw new IllegalArgumentException("Token budget must be greater than zero");
        }
    }
}
