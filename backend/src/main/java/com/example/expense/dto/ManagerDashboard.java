package com.example.expense.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ManagerDashboard(Map<String, Long> countsByStatus, List<CategoryTotal> totalsByCategory,
                               List<MonthlySummary> monthlySummary) {
    public record CategoryTotal(String category, BigDecimal total) {}
    public record MonthlySummary(String month, long count, BigDecimal total) {}
}
