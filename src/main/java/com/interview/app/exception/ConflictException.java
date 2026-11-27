package com.interview.app.exception;

/**
 * Thrown by the service when a request is well-formed but clashes with the current state of the data, . {@link ApiExceptionHandler#conflict} turns it into
 * <b>409 Conflict</b>.
 *
 * <p>Duplicates are usually <i>not</i> reported with this exception: they are caught by the database's unique
 * constraint, which Spring raises as {@code DataIntegrityViolationException} (also mapped to 409).
 * Like {@link NotFoundException}, it is unchecked and rolls back the current transaction.
 */
public class ConflictException extends RuntimeException {

    /** @param message shown to the client, so it should explain what conflicted */
    public ConflictException(String message) { super(message); }
}
