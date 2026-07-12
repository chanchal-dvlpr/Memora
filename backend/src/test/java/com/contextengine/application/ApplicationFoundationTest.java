package com.contextengine.application;

import com.contextengine.application.command.Command;
import com.contextengine.application.command.CommandHandler;
import com.contextengine.application.exception.ApplicationException;
import com.contextengine.application.query.Query;
import com.contextengine.application.query.QueryHandler;
import com.contextengine.application.result.ApplicationResult;
import com.contextengine.application.usecase.UseCase;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;

class ApplicationFoundationTest {

    @Test
    void testApplicationResultSuccess() {
        ApplicationResult<String> result = ApplicationResult.success("Success Value");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isFailure()).isFalse();
        assertThat(result.value()).hasValue("Success Value");
        assertThat(result.error()).isEmpty();

        ApplicationResult<Integer> mapped = result.map(String::length);
        assertThat(mapped.isSuccess()).isTrue();
        assertThat(mapped.value()).hasValue(13);
    }

    @Test
    void testApplicationResultFailure() {
        ApplicationException exception = new ApplicationException("Something went wrong");
        ApplicationResult<String> result = ApplicationResult.failure(exception);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isFailure()).isTrue();
        assertThat(result.value()).isEmpty();
        assertThat(result.error()).hasValue(exception);

        ApplicationResult<Integer> mapped = result.map(String::length);
        assertThat(mapped.isFailure()).isTrue();
        assertThat(mapped.error()).hasValue(exception);
    }

    @Test
    void testApplicationResultIfPresentFlows() {
        AtomicReference<String> successRef = new AtomicReference<>();
        AtomicReference<Throwable> failureRef = new AtomicReference<>();

        ApplicationResult<String> successResult = ApplicationResult.success("Ok");
        successResult.ifPresent(successRef::set, failureRef::set);
        assertThat(successRef.get()).isEqualTo("Ok");
        assertThat(failureRef.get()).isNull();

        successRef.set(null);
        ApplicationException ex = new ApplicationException("Error");
        ApplicationResult<String> failureResult = ApplicationResult.failure(ex);
        failureResult.ifPresent(successRef::set, failureRef::set);
        assertThat(successRef.get()).isNull();
        assertThat(failureRef.get()).isEqualTo(ex);
    }

    @Test
    void testCommandAndQueryContracts() {
        // Implement mock elements to verify handler mappings
        Command mockCommand = new Command() {};
        CommandHandler<Command, String> commandHandler = cmd -> "Handled command";
        assertThat(commandHandler.handle(mockCommand)).isEqualTo("Handled command");

        Query mockQuery = new Query() {};
        QueryHandler<Query, Integer> queryHandler = qry -> 42;
        assertThat(queryHandler.handle(mockQuery)).isEqualTo(42);

        UseCase<String, String> useCase = input -> "Processed " + input;
        assertThat(useCase.execute("data")).isEqualTo("Processed data");
    }
}
