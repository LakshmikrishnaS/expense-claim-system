package com.example.expense.exception;

public class AccountDisabledException extends RuntimeException {
    public AccountDisabledException(String message) { super(message); }
}
