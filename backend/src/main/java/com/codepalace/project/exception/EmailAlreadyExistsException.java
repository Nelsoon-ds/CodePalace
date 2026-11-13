package com.codepalace.project.exception;

/**
 * Thrown when an email address is already registered.
 */
public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("An account already exists for this email address.");
    }
}
