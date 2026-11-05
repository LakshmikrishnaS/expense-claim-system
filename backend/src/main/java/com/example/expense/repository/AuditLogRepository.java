package com.example.expense.repository;

import com.example.expense.entity.ClaimAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<ClaimAuditLog, Long> {
    List<ClaimAuditLog> findByClaimIdOrderByCreatedAtAscIdAsc(Long claimId);
}
