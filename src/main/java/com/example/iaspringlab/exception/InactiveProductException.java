package com.example.iaspringlab.exception;

public class InactiveProductException extends RuntimeException {

    public InactiveProductException(String message) {
        super(message);
    }
}
