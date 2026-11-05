package com.example.expense.controller;

import com.example.expense.dto.EmployeeDashboard;
import com.example.expense.dto.ManagerDashboard;
import com.example.expense.security.UserDetailsImpl;
import com.example.expense.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping("/employee")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public EmployeeDashboard employee(@AuthenticationPrincipal UserDetailsImpl principal) {
        return dashboardService.employee(principal.getUser());
    }

    @GetMapping("/manager")
    @PreAuthorize("hasRole('MANAGER')")
    public ManagerDashboard manager() {
        return dashboardService.manager();
    }
}
