package com.example.expense.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Name is required") @Size(max = 100) String name,
        @NotBlank(message = "Email is required") @Email(message = "Enter a valid email") @Size(max = 150) String email,
        @NotBlank(message = "Password is required") @Size(min = 6, max = 100, message = "Password must be 6-100 characters") String password) {}
