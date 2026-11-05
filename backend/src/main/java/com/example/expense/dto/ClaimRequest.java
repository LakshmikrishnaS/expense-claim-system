package com.example.expense.dto;

import com.example.expense.entity.ClaimCategory;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ClaimRequest(
        @NotBlank(message = "Title is required") @Size(max = 150, message = "Title must be at most 150 characters") String title,
        @NotNull(message = "Amount is required") @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        @Digits(integer = 10, fraction = 2, message = "Amount allows up to 2 decimals") BigDecimal amount,
        @NotNull(message = "Category is required") ClaimCategory category,
        @NotNull(message = "Expense date is required") @PastOrPresent(message = "Expense date cannot be in the future") LocalDate expenseDate,
        @Size(max = 1000, message = "Description must be at most 1000 characters") String description) {}
