package com.interview.app;

import com.interview.app.common.ConflictException;
import com.interview.app.common.NotFoundException;
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
 * One JSON error shape for every failure: {"error": "...", "message": "...", "details": {...}}
 * 400 bad input | 404 unknown id | 409 duplicate / illegal state | 500 unexpected (logged, no stack trace to client)
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> fields.put(e.getField(), e.getDefaultMessage()));
        return body(HttpStatus.BAD_REQUEST, "Validation failed", "One or more fields are invalid", fields);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> badRequest(Exception ex) {
        return body(HttpStatus.BAD_REQUEST, "Malformed request",
                "Body or parameter could not be parsed (check JSON, ids and enum values)", null);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(NotFoundException ex) {
        return body(HttpStatus.NOT_FOUND, "Not found", ex.getMessage(), null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> conflict(ConflictException ex) {
        return body(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), null);
    }

    /** DB constraint backstop (unique, FK, not-null): safe under concurrent requests. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integrity(DataIntegrityViolationException ex) {
        log.info("Constraint violation: {}", ex.getMostSpecificCause().getMessage());
        return body(HttpStatus.CONFLICT, "Conflict", "Request conflicts with existing data (duplicate?)", null);
    }

    /** Framework errors (405, 415, unknown static path...) keep their own status; anything else is a 500. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unexpected(Exception ex) {
        if (ex instanceof ErrorResponse er) {
            HttpStatusCode status = er.getStatusCode();
            return body(status, "Request error", ex.getMessage(), null);
        }
        log.error("Unhandled error", ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", "Unexpected error; see server logs", null);
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatusCode status, String error, String message,
                                                     Map<String, String> details) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("error", error);
        b.put("message", message);
        if (details != null) b.put("details", details);
        return ResponseEntity.status(status).body(b);
    }
}
