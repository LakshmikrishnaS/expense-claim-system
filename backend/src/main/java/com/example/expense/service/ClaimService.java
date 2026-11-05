package com.example.expense.service;

import com.example.expense.dto.AuditLogResponse;
import com.example.expense.dto.ClaimRequest;
import com.example.expense.dto.ClaimResponse;
import com.example.expense.dto.PageResponse;
import com.example.expense.entity.*;
import com.example.expense.exception.BadRequestException;
import com.example.expense.exception.BusinessRuleException;
import com.example.expense.exception.ResourceNotFoundException;
import com.example.expense.repository.ClaimRepository;
import com.example.expense.repository.ClaimSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClaimService {
    private final ClaimRepository claimRepository;
    private final AuditService auditService;
    private final FileStorageService fileStorage;

    public record ReceiptFile(Resource resource, String name, String contentType) {}

    @Transactional
    public ClaimResponse create(ClaimRequest req, User user) {
        Claim claim = Claim.builder()
                .title(req.title().trim()).amount(req.amount()).category(req.category())
                .expenseDate(req.expenseDate()).description(blankToNull(req.description()))
                .status(ClaimStatus.PENDING).employee(user).build();
        claim = claimRepository.save(claim);
        auditService.log(claim, user, AuditAction.CREATED,
                "Created claim \"" + claim.getTitle() + "\" for " + claim.getAmount());
        return ClaimResponse.from(claim);
    }

    @Transactional(readOnly = true)
    public PageResponse<ClaimResponse> listMine(String search, ClaimStatus status, Pageable pageable, User user) {
        Specification<Claim> spec = Specification.where(ClaimSpecifications.employee(user.getId()))
                .and(ClaimSpecifications.status(status))
                .and(ClaimSpecifications.search(search));
        return PageResponse.from(claimRepository.findAll(spec, pageable).map(ClaimResponse::from));
    }

    @Transactional(readOnly = true)
    public ClaimResponse get(Long id, User user) {
        return ClaimResponse.from(getAccessibleClaim(id, user));
    }

    @Transactional
    public ClaimResponse update(Long id, ClaimRequest req, User user) {
        Claim claim = getOwnedClaim(id, user);
        requirePending(claim, "edited");
        StringBuilder changes = new StringBuilder();
        track(changes, "title", claim.getTitle(), req.title().trim());
        track(changes, "amount", claim.getAmount(), req.amount());
        track(changes, "category", claim.getCategory(), req.category());
        track(changes, "expense date", claim.getExpenseDate(), req.expenseDate());
        claim.setTitle(req.title().trim());
        claim.setAmount(req.amount());
        claim.setCategory(req.category());
        claim.setExpenseDate(req.expenseDate());
        claim.setDescription(blankToNull(req.description()));
        claim = claimRepository.save(claim);
        auditService.log(claim, user, AuditAction.UPDATED, changes.isEmpty() ? "Updated claim details" : changes.toString());
        return ClaimResponse.from(claim);
    }

    /** Soft delete: the claim disappears from every list but its audit trail is preserved. */
    @Transactional
    public void delete(Long id, User user) {
        Claim claim = getOwnedClaim(id, user);
        requirePending(claim, "deleted");
        claim.setDeleted(true);
        claimRepository.save(claim);
        auditService.log(claim, user, AuditAction.DELETED, "Deleted claim \"" + claim.getTitle() + "\"");
    }

    @Transactional
    public ClaimResponse uploadReceipt(Long id, MultipartFile file, User user) {
        Claim claim = getOwnedClaim(id, user);
        requirePending(claim, "changed");
        FileStorageService.StoredFile stored = fileStorage.store(file);
        String oldFile = claim.getReceiptPath();
        claim.setReceiptPath(stored.storedName());
        claim.setReceiptOriginalName(stored.originalName());
        claim.setReceiptContentType(stored.contentType());
        claim = claimRepository.save(claim);
        fileStorage.delete(oldFile);
        auditService.log(claim, user, AuditAction.RECEIPT_UPLOADED, "Attached receipt " + stored.originalName());
        return ClaimResponse.from(claim);
    }

    @Transactional(readOnly = true)
    public ReceiptFile downloadReceipt(Long id, User user) {
        Claim claim = getAccessibleClaim(id, user);
        if (claim.getReceiptPath() == null) throw new ResourceNotFoundException("This claim has no receipt");
        return new ReceiptFile(fileStorage.load(claim.getReceiptPath()),
                claim.getReceiptOriginalName(), claim.getReceiptContentType());
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> auditLogs(Long id, User user) {
        Claim claim = getAccessibleClaim(id, user);
        return auditService.timeline(claim.getId());
    }

    /** Employees only ever see their own claims: another user's claim simply "does not exist" (404). */
    Claim getAccessibleClaim(Long id, User user) {
        if (user.getRole() == Role.MANAGER) {
            return claimRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Claim not found"));
        }
        return getOwnedClaim(id, user);
    }

    private Claim getOwnedClaim(Long id, User user) {
        return claimRepository.findByIdAndEmployeeId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found"));
    }

    private void requirePending(Claim claim, String verb) {
        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new BusinessRuleException("Only pending claims can be " + verb);
        }
    }

    private static void track(StringBuilder sb, String field, Object before, Object after) {
        if (before != null && !before.equals(after)) {
            if (!sb.isEmpty()) sb.append("; ");
            sb.append(field).append(": ").append(before).append(" -> ").append(after);
        }
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
