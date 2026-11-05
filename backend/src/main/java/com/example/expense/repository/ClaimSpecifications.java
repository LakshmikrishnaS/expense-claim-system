package com.example.expense.repository;

import com.example.expense.entity.Claim;
import com.example.expense.entity.ClaimStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class ClaimSpecifications {
    private ClaimSpecifications() {}

    public static Specification<Claim> employee(Long employeeId) {
        return (root, q, cb) -> employeeId == null ? null : cb.equal(root.get("employee").get("id"), employeeId);
    }

    public static Specification<Claim> status(ClaimStatus status) {
        return (root, q, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Claim> search(String text) {
        return (root, q, cb) -> {
            if (text == null || text.isBlank()) return null;
            String like = "%" + text.trim().toLowerCase() + "%";
            return cb.or(cb.like(cb.lower(root.get("title")), like),
                         cb.like(cb.lower(root.get("description")), like));
        };
    }

    public static Specification<Claim> from(LocalDate from) {
        return (root, q, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("expenseDate"), from);
    }

    public static Specification<Claim> to(LocalDate to) {
        return (root, q, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("expenseDate"), to);
    }
}
