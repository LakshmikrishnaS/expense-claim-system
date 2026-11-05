package com.example.expense.service;

import com.example.expense.dto.EmployeeDashboard;
import com.example.expense.dto.ManagerDashboard;
import com.example.expense.entity.ClaimStatus;
import com.example.expense.entity.User;
import com.example.expense.repository.ClaimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final ClaimRepository claimRepository;

    @Transactional(readOnly = true)
    public EmployeeDashboard employee(User user) {
        BigDecimal pending = BigDecimal.ZERO, approved = BigDecimal.ZERO, rejected = BigDecimal.ZERO;
        long count = 0;
        for (Object[] row : claimRepository.employeeTotals(user.getId())) {
            ClaimStatus status = (ClaimStatus) row[0];
            count += ((Number) row[1]).longValue();
            BigDecimal sum = toBig(row[2]);
            switch (status) {
                case PENDING -> pending = sum;
                case APPROVED -> approved = sum;
                case REJECTED -> rejected = sum;
            }
        }
        return new EmployeeDashboard(pending.add(approved).add(rejected), approved, rejected, pending, count);
    }

    @Transactional(readOnly = true)
    public ManagerDashboard manager() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (ClaimStatus s : ClaimStatus.values()) counts.put(s.name(), 0L);
        for (Object[] row : claimRepository.countByStatus()) {
            counts.put(((ClaimStatus) row[0]).name(), ((Number) row[1]).longValue());
        }
        List<ManagerDashboard.CategoryTotal> categories = claimRepository.totalsByCategory().stream()
                .map(r -> new ManagerDashboard.CategoryTotal(r[0].toString(), toBig(r[1]))).toList();
        List<ManagerDashboard.MonthlySummary> months = claimRepository.monthlySummary().stream()
                .map(r -> new ManagerDashboard.MonthlySummary(
                        String.format("%04d-%02d", ((Number) r[0]).intValue(), ((Number) r[1]).intValue()),
                        ((Number) r[2]).longValue(), toBig(r[3]))).toList();
        return new ManagerDashboard(counts, categories, months);
    }

    private static BigDecimal toBig(Object o) {
        return o instanceof BigDecimal b ? b : new BigDecimal(o.toString());
    }
}
