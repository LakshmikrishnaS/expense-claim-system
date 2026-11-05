package com.example.expense.service;

import com.example.expense.dto.ClaimRequest;
import com.example.expense.dto.ClaimResponse;
import com.example.expense.entity.*;
import com.example.expense.exception.BusinessRuleException;
import com.example.expense.exception.ResourceNotFoundException;
import com.example.expense.repository.ClaimRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClaimServiceTest {
    @Mock ClaimRepository claimRepository;
    @Mock AuditService auditService;
    @Mock FileStorageService fileStorage;
    @InjectMocks ClaimService claimService;

    User employee;
    User otherEmployee;
    User manager;

    @BeforeEach
    void setUp() {
        employee = User.builder().id(1L).name("Emp One").email("e1@x.com").role(Role.EMPLOYEE).active(true).build();
        otherEmployee = User.builder().id(2L).name("Emp Two").email("e2@x.com").role(Role.EMPLOYEE).active(true).build();
        manager = User.builder().id(9L).name("Boss").email("m@x.com").role(Role.MANAGER).active(true).build();
    }

    private Claim claim(ClaimStatus status) {
        return Claim.builder().id(10L).title("Taxi").amount(new BigDecimal("25.00")).category(ClaimCategory.TRAVEL)
                .expenseDate(LocalDate.now()).status(status).employee(employee)
                .createdAt(java.time.Instant.now()).updatedAt(java.time.Instant.now()).build();
    }

    private ClaimRequest request() {
        return new ClaimRequest("Taxi to client", new BigDecimal("30.00"), ClaimCategory.TRAVEL, LocalDate.now(), "desc");
    }

    @Test
    void employeeCannotViewAnotherUsersClaim() {
        when(claimRepository.findByIdAndEmployeeId(10L, 2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> claimService.get(10L, otherEmployee));
        verify(claimRepository, never()).findById(any());
    }

    @Test
    void managerCanViewAnyClaim() {
        when(claimRepository.findById(10L)).thenReturn(Optional.of(claim(ClaimStatus.PENDING)));

        ClaimResponse res = claimService.get(10L, manager);

        assertEquals(10L, res.id());
    }

    @Test
    void cannotEditNonPendingClaim() {
        when(claimRepository.findByIdAndEmployeeId(10L, 1L)).thenReturn(Optional.of(claim(ClaimStatus.APPROVED)));

        assertThrows(BusinessRuleException.class, () -> claimService.update(10L, request(), employee));
        verify(claimRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void cannotDeleteNonPendingClaim() {
        when(claimRepository.findByIdAndEmployeeId(10L, 1L)).thenReturn(Optional.of(claim(ClaimStatus.REJECTED)));

        assertThrows(BusinessRuleException.class, () -> claimService.delete(10L, employee));
        verify(claimRepository, never()).save(any());
    }

    @Test
    void deletingPendingClaimIsSoftDeleteAndAudited() {
        Claim c = claim(ClaimStatus.PENDING);
        when(claimRepository.findByIdAndEmployeeId(10L, 1L)).thenReturn(Optional.of(c));

        claimService.delete(10L, employee);

        assertTrue(c.isDeleted());
        verify(auditService).log(eq(c), eq(employee), eq(AuditAction.DELETED), anyString());
    }

    @Test
    void createStoresPendingClaimAndWritesAuditLog() {
        when(claimRepository.save(any(Claim.class))).thenAnswer(inv -> {
            Claim c = inv.getArgument(0);
            c.setId(77L);
            c.setCreatedAt(java.time.Instant.now());
            c.setUpdatedAt(java.time.Instant.now());
            return c;
        });

        ClaimResponse res = claimService.create(request(), employee);

        assertEquals(ClaimStatus.PENDING, res.status());
        assertEquals(1L, res.employeeId());
        verify(auditService).log(any(Claim.class), eq(employee), eq(AuditAction.CREATED), anyString());
    }
}
