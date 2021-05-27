package com.rbkmoney.fraudbusters.mg.connector.exception;

public class UnknownResourceException extends RuntimeException {

    public UnknownResourceException(String message) {
        super(message);
    }

    public UnknownResourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnknownResourceException(Throwable cause) {
        super(cause);
    }

    public UnknownResourceException(String message, Throwable cause, boolean enableSuppression,
                                    boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
