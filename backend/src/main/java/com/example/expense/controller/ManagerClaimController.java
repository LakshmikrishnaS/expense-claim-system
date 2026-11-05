package com.example.expense.controller;

import com.example.expense.dto.ClaimResponse;
import com.example.expense.dto.PageResponse;
import com.example.expense.dto.ReviewRequest;
import com.example.expense.entity.ClaimStatus;
import com.example.expense.security.UserDetailsImpl;
import com.example.expense.service.ManagerClaimService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/manager/claims")
@PreAuthorize("hasRole('MANAGER')")
@RequiredArgsConstructor
public class ManagerClaimController {
    private final ManagerClaimService managerClaimService;

    @GetMapping
    public PageResponse<ClaimResponse> list(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) ClaimStatus status,
                                            @RequestParam(required = false) Long employeeId,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                            @RequestParam(required = false) String search) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return managerClaimService.list(status, employeeId, from, to, search, pageable);
    }

    @PatchMapping("/{id}/approve")
    public ClaimResponse approve(@PathVariable Long id, @Valid @RequestBody(required = false) ReviewRequest req,
                                 @AuthenticationPrincipal UserDetailsImpl principal) {
        return managerClaimService.review(id, true, req == null ? null : req.comment(), principal.getUser());
    }

    @PatchMapping("/{id}/reject")
    public ClaimResponse reject(@PathVariable Long id, @Valid @RequestBody(required = false) ReviewRequest req,
                                @AuthenticationPrincipal UserDetailsImpl principal) {
        return managerClaimService.review(id, false, req == null ? null : req.comment(), principal.getUser());
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) ClaimStatus status,
                                         @RequestParam(required = false) Long employeeId,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam(required = false) String search) {
        String csv = managerClaimService.exportCsv(status, employeeId, from, to, search);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("claims.csv", StandardCharsets.UTF_8).build().toString())
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }
}
