package com.contextengine.application.scanner;

import java.util.Objects;
import java.util.Optional;

/**
 * Functional result pattern wrapper representing either a successful outcome or an exception.
 * <p>
 * Bounded Context: Workspace Ingestion
 * Related Subsystem: Project Scanner Subsystem (PS-SUB)
 * </p>
 *
 * @param <S> success value type
 * @param <E> exception error type
 */
public final class Result<S, E extends Exception> {

    private final S value;
    private final E error;

    private Result(S value, E error) {
        this.value = value;
        this.error = error;
    }

    /**
     * Creates a successful Result.
     *
     * @param value the successful value
     * @param <S> success value type
     * @param <E> exception error type
     * @return successful Result wrapper
     */
    public static <S, E extends Exception> Result<S, E> success(S value) {
        return new Result<>(value, null);
    }

    /**
     * Creates a failed Result.
     *
     * @param error the failure exception
     * @param <S> success value type
     * @param <E> exception error type
     * @return failed Result wrapper
     */
    public static <S, E extends Exception> Result<S, E> failure(E error) {
        return new Result<>(null, Objects.requireNonNull(error, "Error exception must not be null"));
    }

    /**
     * Checks if the result is successful.
     *
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() {
        return error == null;
    }

    /**
     * Checks if the result is a failure.
     *
     * @return true if failed, false otherwise
     */
    public boolean isFailure() {
        return error != null;
    }

    /**
     * Returns the success value wrapped in an Optional.
     *
     * @return optional value
     */
    public Optional<S> value() {
        return Optional.ofNullable(value);
    }

    /**
     * Returns the exception wrapped in an Optional.
     *
     * @return optional exception
     */
    public Optional<E> error() {
        return Optional.ofNullable(error);
    }
}
