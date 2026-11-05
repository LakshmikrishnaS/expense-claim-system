package com.example.expense.controller;

import com.example.expense.dto.*;
import com.example.expense.entity.ClaimStatus;
import com.example.expense.security.UserDetailsImpl;
import com.example.expense.service.ClaimService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {
    private final ClaimService claimService;

    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public PageResponse<ClaimResponse> list(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String search,
                                            @RequestParam(required = false) ClaimStatus status,
                                            @AuthenticationPrincipal UserDetailsImpl principal) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return claimService.listMine(search, status, pageable, principal.getUser());
    }

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ClaimResponse> create(@Valid @RequestBody ClaimRequest req,
                                                @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(claimService.create(req, principal.getUser()));
    }

    @GetMapping("/{id}")
    public ClaimResponse get(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl principal) {
        return claimService.get(id, principal.getUser());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ClaimResponse update(@PathVariable Long id, @Valid @RequestBody ClaimRequest req,
                                @AuthenticationPrincipal UserDetailsImpl principal) {
        return claimService.update(id, req, principal.getUser());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl principal) {
        claimService.delete(id, principal.getUser());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(path = "/{id}/receipt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ClaimResponse uploadReceipt(@PathVariable Long id, @RequestPart("file") MultipartFile file,
                                       @AuthenticationPrincipal UserDetailsImpl principal) {
        return claimService.uploadReceipt(id, file, principal.getUser());
    }

    @GetMapping("/{id}/receipt")
    public ResponseEntity<Resource> receipt(@PathVariable Long id,
                                            @RequestParam(defaultValue = "false") boolean download,
                                            @AuthenticationPrincipal UserDetailsImpl principal) {
        ClaimService.ReceiptFile f = claimService.downloadReceipt(id, principal.getUser());
        ContentDisposition cd = (download ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(f.name(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(f.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(f.resource());
    }

    @GetMapping("/{id}/audit-logs")
    public List<AuditLogResponse> auditLogs(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl principal) {
        return claimService.auditLogs(id, principal.getUser());
    }
}
