package com.interview.app.exception;

/**
 * Thrown by the service when a requested record doesn't exist; {@link ApiExceptionHandler#notFound} turns it into
 * <b>404 Not Found</b>.
 *
 * <p>It extends {@code RuntimeException}, an <i>unchecked</i> exception, so methods don't have to declare
 * {@code throws NotFoundException}. It travels up through the service and controller automatically until the handler
 * catches it. Throwing it also makes an open {@code @Transactional} method roll back.
 */
public class NotFoundException extends RuntimeException {

    /** @param message shown to the client in the {@code message} field, e.g. "Item 42 not found" */
    public NotFoundException(String message) { super(message); }
}
