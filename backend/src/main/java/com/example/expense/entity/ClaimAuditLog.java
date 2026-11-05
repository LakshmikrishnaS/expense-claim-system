package com.example.expense.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "claim_audit_logs", indexes = @Index(name = "idx_audit_claim", columnList = "claim_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClaimAuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Plain id (no FK) so the log survives soft/hard deletion of the claim. */
    @Column(name = "claim_id", nullable = false)
    private Long claimId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuditAction action;

    @Column(length = 1000)
    private String details;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}
