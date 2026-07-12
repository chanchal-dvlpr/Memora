package com.contextengine.application.command;

/**
 * Executes state-modifying Commands and returns results.
 *
 * @param <C> the specific Command type
 * @param <R> the output type
 */
public interface CommandHandler<C extends Command, R> {
    
    /**
     * Handles command execution.
     *
     * @param command the command payload
     * @return the execution outcome details
     */
    R handle(C command);
}
