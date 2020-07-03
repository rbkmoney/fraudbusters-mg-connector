package com.rbkmoney.fraudbusters.mg.connector.exception;

public class PaymentInfoRequestException extends RuntimeException {
    public PaymentInfoRequestException() {
    }

    public PaymentInfoRequestException(String message) {
        super(message);
    }

    public PaymentInfoRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public PaymentInfoRequestException(Throwable cause) {
        super(cause);
    }

    public PaymentInfoRequestException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
