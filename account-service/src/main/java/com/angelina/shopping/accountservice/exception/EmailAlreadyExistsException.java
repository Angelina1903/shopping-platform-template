package com.angelina.shopping.accountservice.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("email already exists: " + email);
    }
}