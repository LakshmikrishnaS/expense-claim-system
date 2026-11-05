package com.example.expense.dto;

public record AuthResponse(String accessToken, String refreshToken, UserResponse user) {}
