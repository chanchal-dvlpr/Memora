package com.contextengine.api.advice;

import com.contextengine.application.exception.ApplicationException;
import com.contextengine.application.knowledge.exception.KnowledgeException;
import com.contextengine.domain.service.BudgetUnderflowException;
import com.contextengine.domain.service.DirectoryAccessDeniedException;
import com.contextengine.domain.service.EventQueueOverflowException;
import com.contextengine.domain.service.GraphIntegrityViolationException;
import com.contextengine.domain.service.GraphUnreachableException;
import com.contextengine.domain.service.IndexOutOfSyncException;
import com.contextengine.domain.service.InvalidADRFormatException;
import com.contextengine.domain.service.ManifestParseException;
import com.contextengine.domain.service.OverlappingProjectException;
import com.contextengine.domain.service.ScannerThreadExhaustionException;
import com.contextengine.domain.validation.ValidationException;
import com.contextengine.application.exception.ProjectAlreadyRegisteredException;
import com.contextengine.application.exception.ProjectPathOverlapsException;
import com.contextengine.application.exception.DirectoryAccessDeniedApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST boundary exception interceptor translating internal system/domain exceptions
 * into standardized, client-safe error payload messages.
 * <p>
 * Bounded Context: REST Presentation Layer
 * Reference: Section 5.18 Error Model
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private String resolveCorrelationId() {
        return com.contextengine.application.event.EventContext.correlationId().toString();
    }

    /**
     * Handles payload validation schema failures thrown by the REST boundary validation interceptor.
     *
     * @param ex the validation exception containing field validation errors
     * @return REST error payload response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorPayload> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<ErrorFieldDetails> details = ex.getBindingResult().getFieldErrors().stream()
            .map(err -> new ErrorFieldDetails(err.getField(), err.getDefaultMessage()))
            .collect(Collectors.toList());

        ErrorPayload payload = new ErrorPayload(
            "VALIDATION",
            "INVALID_REQUEST_PARAMETERS",
            "Request parameters failed boundary schema validation check.",
            resolveCorrelationId(),
            Instant.now().toString(),
            details
        );
        return new ResponseEntity<>(payload, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles domain validation failures.
     *
     * @param ex the domain validation exception
     * @return REST error payload response
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorPayload> handleValidationException(ValidationException ex) {
        ErrorPayload payload = new ErrorPayload(
            "VALIDATION",
            "VALIDATION_FAILURE",
            ex.getMessage(),
            resolveCorrelationId(),
            Instant.now().toString(),
            new ArrayList<>()
        );
        return new ResponseEntity<>(payload, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles core SecurityExceptions.
     *
     * @param ex the security exception
     * @return REST error payload response
     */
    @ExceptionHandler(com.contextengine.security.foundation.SecurityException.class)
    public ResponseEntity<ErrorPayload> handleSecurityException(com.contextengine.security.foundation.SecurityException ex) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        String code = "FORBIDDEN";

        if (ex.getErrorCode() == com.contextengine.security.foundation.SecurityConstants.ERROR_AUTHENTICATION_FAILED) {
            status = HttpStatus.UNAUTHORIZED;
            code = "UNAUTHORIZED";
        } else if (ex.getErrorCode() == com.contextengine.security.foundation.SecurityConstants.ERROR_SCOPE_INSUFFICIENT) {
            status = HttpStatus.FORBIDDEN;
            code = "INSUFFICIENT_PRIVILEGES";
        } else if (ex.getErrorCode() == com.contextengine.security.foundation.SecurityConstants.ERROR_WORKSPACE_LOCKED) {
            status = HttpStatus.FORBIDDEN;
            code = "WORKSPACE_LOCKED";
        } else if (ex.getErrorCode() == com.contextengine.security.foundation.SecurityConstants.ERROR_BOUNDARY_VIOLATION) {
            status = HttpStatus.FORBIDDEN;
            code = "BOUNDARY_VIOLATION";
        }

        ErrorPayload payload = new ErrorPayload(
            "SECURITY",
            code,
            ex.getMessage(),
            resolveCorrelationId(),
            Instant.now().toString(),
            new ArrayList<>()
        );
        return new ResponseEntity<>(payload, status);
    }

    /**
     * Handles ADR format violations.
     *
     * @param ex the ADR format exception
     * @return REST error payload response
     */
    @ExceptionHandler(InvalidADRFormatException.class)
    public ResponseEntity<ErrorPayload> handleInvalidADRFormatException(InvalidADRFormatException ex) {
        ErrorPayload payload = new ErrorPayload(
            "VALIDATION",
            "INVALID_ADR_FORMAT",
            ex.getMessage(),
            resolveCorrelationId(),
            Instant.now().toString(),
            new ArrayList<>()
        );
        return new ResponseEntity<>(payload, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles overlapping project registrations path violations.
     *
     * @param ex the path overlap exception
     * @return REST error payload response
     */
    @ExceptionHandler(OverlappingProjectException.class)
    public ResponseEntity<ErrorPayload> handleOverlappingProject(OverlappingProjectException ex) {
        ErrorPayload payload = new ErrorPayload(
            "INVARIANT_VIOLATION",
            "PATH_ALREADY_REGISTERED",
            ex.getMessage(),
            resolveCorrelationId(),
            Instant.now().toString(),
            new ArrayList<>()
        );
        return new ResponseEntity<>(payload, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(ProjectAlreadyRegisteredException.class)
    public ResponseEntity<ErrorPayload> handleProjectAlreadyRegistered(ProjectAlreadyRegisteredException ex) {
        ErrorPayload payload = new ErrorPayload(
            "INVARIANT_VIOLATION",
            "PROJECT_ALREADY_REGISTERED",
            ex.getMessage(),
            resolveCorrelationId(),
            Instant.now().toString(),
            new ArrayList<>()
        );
        return new ResponseEntity<>(payload, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ProjectPathOverlapsException.class)
    public ResponseEntity<ErrorPayload> handleProjectPathOverlaps(ProjectPathOverlapsException ex) {
        ErrorPayload payload = new ErrorPayload(
            "INVARIANT_VIOLATION",
            "PATH_ALREADY_REGISTERED",
            ex.getMessage(),
            resolveCorrelationId(),
            Instant.now().toString(),
            new ArrayList<>()
        );
        return new ResponseEntity<>(payload, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(DirectoryAccessDeniedApplicationException.class)
    public ResponseEntity<ErrorPayload> handleDirectoryAccessDeniedApplication(DirectoryAccessDeniedApplicationException ex) {
        ErrorPayload payload = new ErrorPayload(
            "SECURITY",
            "DIRECTORY_ACCESS_DENIED",
            ex.getMessage(),
            resolveCorrelationId(),
            Instant.now().toString(),
            new ArrayList<>()
        );
        return new ResponseEntity<>(payload, HttpStatus.FORBIDDEN);
    }

    /**
     * Handles token budget size violations.
     *
     * @param ex the budget underflow exception
     * @return REST error payload response
     */
    @ExceptionHandler(BudgetUnderflowException.class)
    public ResponseEntity<ErrorPayload> handleBudgetUnderflow(BudgetUnderflowException ex) {
        ErrorPayload payload = new ErrorPayload(
            "INVARIANT_VIOLATION",
            "BUDGET_TOO_SMALL",
            ex.getMessage(),
            resolveCorrelationId(),
            Instant.now().toString(),
            new ArrayList<>()
        );
        return new ResponseEntity<>(payload, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * Handles graph relational constraints violations.
     *
     * @param ex the graph constraint exception
     * @return REST error payload response
     */
    @ExceptionHandler(GraphIntegrityViolationException.class)
    public ResponseEntity<ErrorPayload> handleGraphIntegrity(GraphIntegrityViolationException ex) {
        ErrorPayload payload = new ErrorPayload(
            "INVARIANT_VIOLATION",
            "GRAPH_INTEGRITY_VIOLATION",
            ex.getMessage(),
            resolveCorrelationId(),
            Instant.now().toString(),
            new ArrayList<>()
        );
        return new ResponseEntity<>(payload, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * Handles stale index queries.
     *
     * @param ex the index sync exception
     * @return REST error payload response
     */
    @ExceptionHandler(IndexOutOfSyncException.class)
    public ResponseEntity<ErrorPayload> handleIndexOutOfSync(IndexOutOfSyncException ex) {
        ErrorPayload payload = new ErrorPayload(
            "INVARIANT_VIOLATION",
            "INDEX_OUT_OF_SYNC",
            ex.getMessage(),
            resolveCorrelationId(),
            Instant.now().toString(),
            new ArrayList<>()
        );
        return new ResponseEntity<>(payload, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * Handles filesystem permissions rejections.
     *
     * @param ex the access denied exception
     * @return REST error payload response
     */
    @ExceptionHandler(DirectoryAccessDeniedException.class)
    public ResponseEntity<ErrorPayload> handleAccessDenied(DirectoryAccessDeniedException ex) {
        ErrorPayload payload = new ErrorPayload(
            "SECURITY",
            "DIRECTORY_ACCESS_DENIED",
            ex.getMessage(),
            resolveCorrelationId(),
            Instant.now().toString(),
            new ArrayList<>()
        );
        return new ResponseEntity<>(payload, HttpStatus.FORBIDDEN);
    }

    /**
     * Handles unreachable storage resources.
     *
     * @param ex the graph unreachable exception
     * @return REST error payload response
     */
    @ExceptionHandler(GraphUnreachableException.class)
    public ResponseEntity<ErrorPayload> handleGraphUnreachable(GraphUnreachableException ex) {
        ErrorPayload payload = new ErrorPayload(
            "SYSTEM",
            "GRAPH_UNREACHABLE",
            ex.getMessage(),
            resolveCorrelationId(),
            Instant.now().toString(),
            new ArrayList<>()
        );
        return new ResponseEntity<>(payload, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handles thread resource starvation.
     *
     * @param ex the thread exhaustion exception
     * @return REST error payload response
     */
    @ExceptionHandler(ScannerThreadExhaustionException.class)
    public ResponseEntity<ErrorPayload> handleThreadExhaustion(ScannerThreadExhaustionException ex) {
        ErrorPayload payload = new ErrorPayload(
            "SYSTEM",
            "SCANNER_THREAD_EXHAUSTION",
            ex.getMessage(),
            resolveCorrelationId(),
            Instant.now().toString(),
            new ArrayList<>()
        );
        return new ResponseEntity<>(payload, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handles event bus queue capacity overflow.
     *
     * @param ex the queue overflow exception
     * @return REST error payload response
     */
    @ExceptionHandler(EventQueueOverflowException.class)
    public ResponseEntity<ErrorPayload> handleQueueOverflow(EventQueueOverflowException ex) {
        ErrorPayload payload = new ErrorPayload(
            "SYSTEM",
            "EVENT_QUEUE_OVERFLOW",
            ex.getMessage(),
            resolveCorrelationId(),
            Instant.now().toString(),
            new ArrayList<>()
        );
        return new ResponseEntity<>(payload, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handles codebase configuration file parsing failures.
     *
     * @param ex the manifest parse exception
     * @return REST error payload response
     */
    @ExceptionHandler(ManifestParseException.class)
    public ResponseEntity<ErrorPayload> handleManifestParse(ManifestParseException ex) {
        ErrorPayload payload = new ErrorPayload(
            "SYSTEM",
            "MANIFEST_PARSE_FAILURE",
            ex.getMessage(),
            resolveCorrelationId(),
            Instant.now().toString(),
            new ArrayList<>()
        );
        return new ResponseEntity<>(payload, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handles KnowledgeEngine specific processing exceptions.
     *
     * @param ex the knowledge exception
     * @return REST error payload response
     */
    @ExceptionHandler(KnowledgeException.class)
    public ResponseEntity<ErrorPayload> handleKnowledgeException(KnowledgeException ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String category = "SYSTEM";
        String code = ex.errorCode();

        if ("ERR_SCAN_PROJECT_LOCKED".equals(code) || "SCAN_PROJECT_LOCKED".equals(code)) {
            status = HttpStatus.UNPROCESSABLE_ENTITY;
            category = "INVARIANT_VIOLATION";
            code = "SCAN_PROJECT_LOCKED";
        }

        ErrorPayload payload = new ErrorPayload(
            category,
            code,
            ex.getMessage(),
            resolveCorrelationId(),
            Instant.now().toString(),
            new ArrayList<>()
        );
        return new ResponseEntity<>(payload, status);
    }

    /**
     * Handles general application execution exceptions.
     *
     * @param ex the application exception
     * @return REST error payload response
     */
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorPayload> handleApplicationException(ApplicationException ex) {
        ErrorPayload payload = new ErrorPayload(
            "SYSTEM",
            "APPLICATION_ERROR",
            ex.getMessage(),
            resolveCorrelationId(),
            Instant.now().toString(),
            new ArrayList<>()
        );
        return new ResponseEntity<>(payload, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handles project lookup missing failures.
     *
     * @param ex the project not found exception
     * @return REST error payload response
     */
    @ExceptionHandler(com.contextengine.application.exception.ProjectNotFoundException.class)
    public ResponseEntity<ErrorPayload> handleProjectNotFound(com.contextengine.application.exception.ProjectNotFoundException ex) {
        ErrorPayload payload = new ErrorPayload(
            "INVARIANT_VIOLATION",
            "PROJECT_NOT_FOUND",
            ex.getMessage(),
            resolveCorrelationId(),
            Instant.now().toString(),
            new ArrayList<>()
        );
        return new ResponseEntity<>(payload, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles snapshot history lookup missing failures.
     *
     * @param ex the snapshot not found exception
     * @return REST error payload response
     */
    @ExceptionHandler(com.contextengine.application.exception.SnapshotNotFoundException.class)
    public ResponseEntity<ErrorPayload> handleSnapshotNotFound(com.contextengine.application.exception.SnapshotNotFoundException ex) {
        ErrorPayload payload = new ErrorPayload(
            "INVARIANT_VIOLATION",
            "SNAPSHOT_NOT_FOUND",
            ex.getMessage(),
            resolveCorrelationId(),
            Instant.now().toString(),
            new ArrayList<>()
        );
        return new ResponseEntity<>(payload, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles type mismatches in request parameters or path variables (e.g., malformed UUID).
     */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorPayload> handleMethodArgumentTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        String message = ex.getMessage();
        if (ex.getRequiredType() == java.util.UUID.class) {
            message = "Invalid UUID string: " + ex.getValue();
        }
        ErrorPayload payload = new ErrorPayload(
            "VALIDATION",
            "INVALID_UUID",
            message,
            resolveCorrelationId(),
            Instant.now().toString(),
            new ArrayList<>()
        );
        return new ResponseEntity<>(payload, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles invalid argument exceptions (e.g., manual UUID parsing failure).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorPayload> handleIllegalArgumentException(IllegalArgumentException ex) {
        String msg = ex.getMessage();
        String code = "INVALID_ARGUMENT";
        if (msg != null && msg.contains("Invalid UUID")) {
            code = "INVALID_UUID";
        }
        ErrorPayload payload = new ErrorPayload(
            "VALIDATION",
            code,
            msg,
            resolveCorrelationId(),
            Instant.now().toString(),
            new ArrayList<>()
        );
        return new ResponseEntity<>(payload, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles route and static resource lookup failures, mapping them to 404 Not Found.
     *
     * @param ex the route missing exception
     * @return REST error payload response
     */
    @ExceptionHandler({
        org.springframework.web.servlet.resource.NoResourceFoundException.class,
        org.springframework.web.servlet.NoHandlerFoundException.class
    })
    public ResponseEntity<ErrorPayload> handleRouteNotFound(Exception ex) {
        ErrorPayload payload = new ErrorPayload(
            "SYSTEM",
            "ROUTE_NOT_FOUND",
            ex.getMessage() != null ? ex.getMessage() : "The requested REST endpoint path or static resource does not exist.",
            resolveCorrelationId(),
            Instant.now().toString(),
            new ArrayList<>()
        );
        return new ResponseEntity<>(payload, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles all fallback unhandled runtime and system execution exceptions.
     *
     * @param ex the unhandled exception
     * @return REST error payload response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorPayload> handleGeneralException(Exception ex) {
        ErrorPayload payload = new ErrorPayload(
            "SYSTEM",
            "INTERNAL_SERVER_ERROR",
            ex.getMessage() != null ? ex.getMessage() : "An unexpected server-side execution error occurred.",
            resolveCorrelationId(),
            Instant.now().toString(),
            new ArrayList<>()
        );
        return new ResponseEntity<>(payload, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
