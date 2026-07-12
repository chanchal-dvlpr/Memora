package com.contextengine.application.result;

import com.contextengine.application.exception.ApplicationException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Encapsulates the execution outcome of an application command, query, or use case.
 * Standardizes success values and failure exception propagation.
 *
 * @param <T> the type of success value
 */
public final class ApplicationResult<T> {
    
    private final T value;
    private final ApplicationException error;

    private ApplicationResult(T value, ApplicationException error) {
        this.value = value;
        this.error = error;
    }

    /**
     * Creates a successful result holding the specified value.
     *
     * @param value the output value
     * @param <T> the type of success value
     * @return a successful ApplicationResult
     */
    public static <T> ApplicationResult<T> success(T value) {
        return new ApplicationResult<>(Objects.requireNonNull(value, "Success value must not be null"), null);
    }

    /**
     * Creates a failed result holding the specified exception.
     *
     * @param error the application exception
     * @param <T> the type of success value
     * @return a failed ApplicationResult
     */
    public static <T> ApplicationResult<T> failure(ApplicationException error) {
        return new ApplicationResult<>(null, Objects.requireNonNull(error, "Failure exception must not be null"));
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
     * @return the optional value
     */
    public Optional<T> value() {
        return Optional.ofNullable(value);
    }

    /**
     * Returns the failure exception wrapped in an Optional.
     *
     * @return the optional error
     */
    public Optional<ApplicationException> error() {
        return Optional.ofNullable(error);
    }

    /**
     * Maps the success value using the provided mapper function.
     *
     * @param mapper the mapping function
     * @param <U> the new value type
     * @return the mapped ApplicationResult
     */
    public <U> ApplicationResult<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "Mapper must not be null");
        if (isFailure()) {
            return failure(error);
        }
        try {
            return success(mapper.apply(value));
        } catch (ApplicationException e) {
            return failure(e);
        } catch (Exception e) {
            return failure(new ApplicationException("Unhandled error during mapping", e));
        }
    }

    /**
     * Handles success and failure outcomes fluently.
     *
     * @param successConsumer consumed on success
     * @param failureConsumer consumed on failure
     */
    public void ifPresent(Consumer<? super T> successConsumer, Consumer<ApplicationException> failureConsumer) {
        Objects.requireNonNull(successConsumer, "Success consumer must not be null");
        Objects.requireNonNull(failureConsumer, "Failure consumer must not be null");
        if (isSuccess()) {
            successConsumer.accept(value);
        } else {
            failureConsumer.accept(error);
        }
    }
}
