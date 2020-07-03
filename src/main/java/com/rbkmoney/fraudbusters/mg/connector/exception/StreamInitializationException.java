package com.rbkmoney.fraudbusters.mg.connector.exception;

public class StreamInitializationException extends RuntimeException {
    public StreamInitializationException() {
    }

    public StreamInitializationException(String message) {
        super(message);
    }

    public StreamInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public StreamInitializationException(Throwable cause) {
        super(cause);
    }

    public StreamInitializationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
