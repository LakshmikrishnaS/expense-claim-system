package com.example.expense.dto;

import com.example.expense.entity.Claim;
import com.example.expense.entity.ClaimCategory;
import com.example.expense.entity.ClaimStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ClaimResponse(
        Long id, String title, BigDecimal amount, ClaimCategory category, LocalDate expenseDate,
        String description, ClaimStatus status, String reviewComment, String reviewedByName, Instant reviewedAt,
        boolean hasReceipt, String receiptName, String receiptContentType,
        Long employeeId, String employeeName, Instant createdAt, Instant updatedAt) {

    public static ClaimResponse from(Claim c) {
        return new ClaimResponse(
                c.getId(), c.getTitle(), c.getAmount(), c.getCategory(), c.getExpenseDate(),
                c.getDescription(), c.getStatus(), c.getReviewComment(),
                c.getReviewedBy() == null ? null : c.getReviewedBy().getName(), c.getReviewedAt(),
                c.getReceiptPath() != null, c.getReceiptOriginalName(), c.getReceiptContentType(),
                c.getEmployee().getId(), c.getEmployee().getName(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
