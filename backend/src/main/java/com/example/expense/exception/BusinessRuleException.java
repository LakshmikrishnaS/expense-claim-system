package com.example.expense.exception;

/** A valid request that violates a business rule (e.g. editing a non-pending claim) -> 409. */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) { super(message); }
}
