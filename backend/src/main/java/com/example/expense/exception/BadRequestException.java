package com.example.expense.exception;

/** Invalid input that bean validation cannot express (e.g. missing reject comment, bad file) -> 400. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}
