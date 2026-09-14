package com.marcosperboni.accountservice.exception;

public class UnprocessableBalanceException extends RuntimeException {

    public UnprocessableBalanceException(String message) {
        super(message);
    }
}
