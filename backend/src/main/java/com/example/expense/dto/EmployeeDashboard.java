package com.example.expense.dto;

import java.math.BigDecimal;

public record EmployeeDashboard(BigDecimal totalClaimed, BigDecimal totalApproved, BigDecimal totalRejected,
                                BigDecimal totalPending, long claimCount) {}
