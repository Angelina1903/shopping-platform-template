package com.angelina.shopping.accountservice.exception;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(Long id) {
        super("account not found: " + id);
    }
}