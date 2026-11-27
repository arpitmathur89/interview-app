package com.interview.app.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central error handling: turns every exception into an HTTP status code plus one consistent JSON body.
 * <pre>
 *   {"error": "Not found", "message": "Item 42 not found"}
 *   {"error": "Validation failed", "message": "...", "details": {"name": "must not be blank"}}
 * </pre>
 *
 * <p><b>How it works:</b> {@code @RestControllerAdvice} makes this class apply to every controller. When a controller
 * (or the service it calls) throws, Spring looks here for the {@code @ExceptionHandler} method that matches the
 * exception type most closely, calls it, and sends its return value as the response.
 * Because of this, the service and controller never deal with status codes or try/catch blocks.
 *
 * <p>Mapping used by this app:
 * <pre>
 *   Exception                                   Status  Typical cause
 *   MethodArgumentNotValidException             422     a @Valid field rule failed (blank name, too long)
 *   HttpMessageNotReadableException             400     broken JSON or a wrong JSON type
 *   MethodArgumentTypeMismatchException         400     /api/items/abc (not a number)
 *   NotFoundException (ours)                    404     unknown id
 *   ConflictException (ours)                    409     request clashes with current data
 *   DataIntegrityViolationException             409     DB constraint rejected the write (duplicate name)
 *   other Spring errors (ErrorResponse)         own     405 wrong method, 415 wrong content type, ...
 *   anything else                               500     a bug: logged in full, client gets a generic message
 * </pre>
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** SLF4J logger; output appears in the terminal running the app. */
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /**
     * 422: the JSON was readable but broke a validation rule from {@code ItemDtos}. Collects every bad field into
     * {@code details} so the client can show all problems at once, e.g. {@code {"name": "must not be blank"}}.
     * {@code LinkedHashMap} keeps the fields in the order they were reported.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> fields.put(e.getField(), e.getDefaultMessage()));
        return body(HttpStatus.UNPROCESSABLE_ENTITY, "Validation failed", "One or more fields are invalid", fields);
    }

    /**
     * 400: the request could not even be parsed: invalid JSON, a value that doesn't fit its type (an enum or a
     * number), or a non-numeric id in the URL. The parser's own message is not echoed back because it can expose
     * internal class names.
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> badRequest(Exception ex) {
        return body(HttpStatus.BAD_REQUEST, "Malformed request",
                "Body or parameter could not be parsed (check JSON, ids and enum values)", null);
    }

    /** 404: thrown by the service when an id doesn't exist. Our own message is safe to show. */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(NotFoundException ex) {
        return body(HttpStatus.NOT_FOUND, "Not found", ex.getMessage(), null);
    }

    /** 409: thrown by the service when the request clashes with current state, e.g. APPROVED to PENDING. */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> conflict(ConflictException ex) {
        return body(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), null);
    }

    /**
     * 409: the database refused a write because of a constraint (unique name, foreign key, NOT NULL). This is the
     * safety net that makes duplicate checks correct even under concurrent requests. The real DB message is logged
     * for debugging but not sent to the client, since it reveals table and index names.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integrity(DataIntegrityViolationException ex) {
        log.info("Constraint violation: {}", ex.getMostSpecificCause().getMessage());
        return body(HttpStatus.CONFLICT, "Conflict", "Request conflicts with existing data (duplicate?)", null);
    }

    /**
     * Catch-all. Spring's own web errors ({@code ErrorResponse}: 405 wrong HTTP method, 415 wrong content type,
     * 404 unknown path...) keep their proper status. Anything else is an unexpected bug: log the full stack
     * trace on the server and return a generic 500, so internals are never leaked to the client.
     * ({@code ex instanceof ErrorResponse er} checks the type and names the cast variable {@code er} in one step.)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unexpected(Exception ex) {
        if (ex instanceof ErrorResponse er) {
            HttpStatusCode status = er.getStatusCode();
            return body(status, "Request error", ex.getMessage(), null);
        }
        log.error("Unhandled error", ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", "Unexpected error; see server logs", null);
    }

    /** Builds the shared {@code {error, message, details?}} JSON body; {@code details} is left out when null. */
    private ResponseEntity<Map<String, Object>> body(HttpStatusCode status, String error, String message,
                                                     Map<String, String> details) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("error", error);
        b.put("message", message);
        if (details != null) b.put("details", details);
        return ResponseEntity.status(status).body(b);
    }
}
