package com.pulsepass.exception;

/** Login fallido. El mensaje es único a propósito: no revela si la cuenta existe. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid credentials");
    }
}