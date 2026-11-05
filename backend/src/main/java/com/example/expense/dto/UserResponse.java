package com.example.expense.dto;

import com.example.expense.entity.Role;
import com.example.expense.entity.User;

import java.time.Instant;

public record UserResponse(Long id, String name, String email, Role role, boolean active, Instant createdAt) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.isActive(), u.getCreatedAt());
    }
}
