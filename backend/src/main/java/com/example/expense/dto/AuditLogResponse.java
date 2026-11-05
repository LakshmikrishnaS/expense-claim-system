package com.example.expense.dto;

import com.example.expense.entity.AuditAction;
import com.example.expense.entity.ClaimAuditLog;
import com.example.expense.entity.Role;

import java.time.Instant;

public record AuditLogResponse(Long id, AuditAction action, String actorName, Role actorRole, String details, Instant createdAt) {
    public static AuditLogResponse from(ClaimAuditLog l) {
        return new AuditLogResponse(l.getId(), l.getAction(), l.getActor().getName(), l.getActor().getRole(),
                l.getDetails(), l.getCreatedAt());
    }
}
