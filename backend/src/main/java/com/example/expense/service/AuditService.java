package com.example.expense.service;

import com.example.expense.dto.AuditLogResponse;
import com.example.expense.entity.AuditAction;
import com.example.expense.entity.Claim;
import com.example.expense.entity.ClaimAuditLog;
import com.example.expense.entity.User;
import com.example.expense.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Append-only audit trail: who did what, and when, for every claim action. */
@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public void log(Claim claim, User actor, AuditAction action, String details) {
        auditLogRepository.save(ClaimAuditLog.builder()
                .claimId(claim.getId()).actor(actor).action(action).details(details).build());
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> timeline(Long claimId) {
        return auditLogRepository.findByClaimIdOrderByCreatedAtAscIdAsc(claimId)
                .stream().map(AuditLogResponse::from).toList();
    }
}
