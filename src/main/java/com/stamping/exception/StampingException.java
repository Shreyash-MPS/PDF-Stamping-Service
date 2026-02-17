package com.stamping.exception;

public class StampingException extends RuntimeException {

    public StampingException(String message) {
        super(message);
    }

    public StampingException(String message, Throwable cause) {
        super(message, cause);
    }
}
