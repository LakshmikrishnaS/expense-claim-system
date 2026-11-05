package com.example.expense.service;

import com.example.expense.dto.ClaimResponse;
import com.example.expense.dto.PageResponse;
import com.example.expense.entity.*;
import com.example.expense.exception.BadRequestException;
import com.example.expense.exception.BusinessRuleException;
import com.example.expense.exception.ResourceNotFoundException;
import com.example.expense.repository.ClaimRepository;
import com.example.expense.repository.ClaimSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManagerClaimService {
    private final ClaimRepository claimRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<ClaimResponse> list(ClaimStatus status, Long employeeId, LocalDate from, LocalDate to,
                                            String search, Pageable pageable) {
        return PageResponse.from(claimRepository.findAll(spec(status, employeeId, from, to, search), pageable)
                .map(ClaimResponse::from));
    }

    /** Approve or reject. A reviewed claim is final; rejecting requires a comment. */
    @Transactional
    public ClaimResponse review(Long id, boolean approve, String comment, User manager) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found"));
        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new BusinessRuleException("This claim has already been " + claim.getStatus().name().toLowerCase()
                    + " and cannot be changed");
        }
        String trimmed = comment == null ? null : comment.trim();
        if (!approve && (trimmed == null || trimmed.isEmpty())) {
            throw new BadRequestException("A comment is required when rejecting a claim");
        }
        claim.setStatus(approve ? ClaimStatus.APPROVED : ClaimStatus.REJECTED);
        claim.setReviewComment(trimmed == null || trimmed.isEmpty() ? null : trimmed);
        claim.setReviewedBy(manager);
        claim.setReviewedAt(Instant.now());
        claim = claimRepository.save(claim);
        auditService.log(claim, manager, approve ? AuditAction.APPROVED : AuditAction.REJECTED,
                claim.getReviewComment() == null ? (approve ? "Approved" : "Rejected") : claim.getReviewComment());
        return ClaimResponse.from(claim);
    }

    @Transactional(readOnly = true)
    public String exportCsv(ClaimStatus status, Long employeeId, LocalDate from, LocalDate to, String search) {
        List<Claim> claims = claimRepository.findAll(spec(status, employeeId, from, to, search),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        StringBuilder sb = new StringBuilder("ID,Employee,Title,Category,Amount,Expense Date,Status,Reviewed By,Review Comment,Submitted At\n");
        for (Claim c : claims) {
            sb.append(c.getId()).append(',')
              .append(cell(c.getEmployee().getName())).append(',')
              .append(cell(c.getTitle())).append(',')
              .append(c.getCategory()).append(',')
              .append(c.getAmount()).append(',')
              .append(c.getExpenseDate()).append(',')
              .append(c.getStatus()).append(',')
              .append(cell(c.getReviewedBy() == null ? "" : c.getReviewedBy().getName())).append(',')
              .append(cell(c.getReviewComment())).append(',')
              .append(c.getCreatedAt()).append('\n');
        }
        return sb.toString();
    }

    private Specification<Claim> spec(ClaimStatus status, Long employeeId, LocalDate from, LocalDate to, String search) {
        return Specification.where(ClaimSpecifications.status(status))
                .and(ClaimSpecifications.employee(employeeId))
                .and(ClaimSpecifications.from(from))
                .and(ClaimSpecifications.to(to))
                .and(ClaimSpecifications.search(search));
    }

    /** Escapes quotes and neutralises spreadsheet formula injection (=, +, -, @). */
    private static String cell(String v) {
        if (v == null) return "";
        String s = v;
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        return "\"" + s.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + "\"";
    }
}
