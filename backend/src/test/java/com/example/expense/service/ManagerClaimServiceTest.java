package com.example.expense.service;

import com.example.expense.dto.ClaimResponse;
import com.example.expense.entity.*;
import com.example.expense.exception.BadRequestException;
import com.example.expense.exception.BusinessRuleException;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ManagerClaimServiceTest {
    @Mock ClaimRepository claimRepository;
    @Mock AuditService auditService;
    @InjectMocks ManagerClaimService service;

    User employee;
    User manager;

    @BeforeEach
    void setUp() {
        employee = User.builder().id(1L).name("Emp").email("e@x.com").role(Role.EMPLOYEE).active(true).build();
        manager = User.builder().id(9L).name("Boss").email("m@x.com").role(Role.MANAGER).active(true).build();
        when(claimRepository.save(any(Claim.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Claim claim(ClaimStatus status) {
        return Claim.builder().id(10L).title("Hotel").amount(new BigDecimal("120.00")).category(ClaimCategory.ACCOMMODATION)
                .expenseDate(LocalDate.now()).status(status).employee(employee)
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
    }

    @Test
    void rejectWithoutCommentFails() {
        when(claimRepository.findById(10L)).thenReturn(Optional.of(claim(ClaimStatus.PENDING)));

        assertThrows(BadRequestException.class, () -> service.review(10L, false, "   ", manager));
        verify(claimRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void rejectWithCommentSucceedsAndIsAudited() {
        Claim c = claim(ClaimStatus.PENDING);
        when(claimRepository.findById(10L)).thenReturn(Optional.of(c));

        ClaimResponse res = service.review(10L, false, "Missing invoice", manager);

        assertEquals(ClaimStatus.REJECTED, res.status());
        assertEquals("Missing invoice", res.reviewComment());
        assertEquals("Boss", res.reviewedByName());
        verify(auditService).log(eq(c), eq(manager), eq(AuditAction.REJECTED), eq("Missing invoice"));
    }

    @Test
    void approveWithoutCommentSucceeds() {
        when(claimRepository.findById(10L)).thenReturn(Optional.of(claim(ClaimStatus.PENDING)));

        ClaimResponse res = service.review(10L, true, null, manager);

        assertEquals(ClaimStatus.APPROVED, res.status());
    }

    @Test
    void alreadyReviewedClaimCannotBeChangedAgain() {
        when(claimRepository.findById(10L)).thenReturn(Optional.of(claim(ClaimStatus.APPROVED)));

        assertThrows(BusinessRuleException.class, () -> service.review(10L, false, "Changed my mind", manager));
        verify(claimRepository, never()).save(any());
    }
}
