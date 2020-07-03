package com.rbkmoney.fraudbusters.mg.connector.exception;

public class PaymentInfoNotFoundException extends RuntimeException {
    public PaymentInfoNotFoundException() {
    }

    public PaymentInfoNotFoundException(String message) {
        super(message);
    }

    public PaymentInfoNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public PaymentInfoNotFoundException(Throwable cause) {
        super(cause);
    }

    public PaymentInfoNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
