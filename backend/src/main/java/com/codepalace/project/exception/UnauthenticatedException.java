package com.codepalace.project.exception;

/**
 * Thrown when a request needs an authenticated caller but none is present.
 */
public class UnauthenticatedException extends RuntimeException {
    public UnauthenticatedException(String message) {
        super(message);
    }
}
