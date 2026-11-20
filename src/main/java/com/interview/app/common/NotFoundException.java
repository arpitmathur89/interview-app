package com.interview.app.common;

/** Maps to 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) { super(message); }
}
