package com.interview.app.common;

/** Maps to 409: duplicate, or the request conflicts with current state (e.g. illegal transition). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}
