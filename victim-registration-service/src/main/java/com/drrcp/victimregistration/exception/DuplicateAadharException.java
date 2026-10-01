package com.drrcp.victimregistration.exception;

public class DuplicateAadharException extends RuntimeException {
    public DuplicateAadharException(String message) {
        super(message);
    }
}