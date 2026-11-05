package com.example.expense.service;

import com.example.expense.dto.AuthResponse;
import com.example.expense.dto.LoginRequest;
import com.example.expense.dto.RefreshRequest;
import com.example.expense.dto.RegisterRequest;
import com.example.expense.entity.RefreshToken;
import com.example.expense.entity.Role;
import com.example.expense.entity.User;
import com.example.expense.exception.AccountDisabledException;
import com.example.expense.exception.BusinessRuleException;
import com.example.expense.exception.InvalidTokenException;
import com.example.expense.repository.RefreshTokenRepository;
import com.example.expense.repository.UserRepository;
import com.example.expense.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {
    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @InjectMocks AuthService authService;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(any())).thenReturn("HASHED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(5L);
            return u;
        });
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.refreshTokenExpiry()).thenReturn(Instant.now().plus(7, ChronoUnit.DAYS));
    }

    @Test
    void registerAlwaysCreatesEmployeeWithHashedPassword() {
        when(userRepository.existsByEmailIgnoreCase("new@x.com")).thenReturn(false);

        AuthResponse res = authService.register(new RegisterRequest("New User", "New@X.com", "secret1"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(Role.EMPLOYEE, captor.getValue().getRole());
        assertEquals("HASHED", captor.getValue().getPassword());
        assertEquals("new@x.com", captor.getValue().getEmail());
        assertTrue(captor.getValue().isActive());
        assertEquals("access-token", res.accessToken());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("dup@x.com")).thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> authService.register(new RegisterRequest("Dup", "dup@x.com", "secret1")));
        verify(userRepository, never()).save(any());
    }

    @Test
    void deactivatedUserCannotLogIn() {
        User u = User.builder().id(3L).email("off@x.com").password("HASHED").role(Role.EMPLOYEE).active(false).build();
        when(userRepository.findByEmailIgnoreCase("off@x.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("secret1", "HASHED")).thenReturn(true);

        assertThrows(AccountDisabledException.class, () -> authService.login(new LoginRequest("off@x.com", "secret1")));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void wrongPasswordIsRejected() {
        User u = User.builder().id(3L).email("a@x.com").password("HASHED").role(Role.EMPLOYEE).active(true).build();
        when(userRepository.findByEmailIgnoreCase("a@x.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("bad", "HASHED")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(new LoginRequest("a@x.com", "bad")));
    }

    @Test
    void refreshIsBlockedForDeactivatedUser() {
        User u = User.builder().id(3L).email("off@x.com").role(Role.EMPLOYEE).active(false).build();
        RefreshToken t = RefreshToken.builder().token("tok").user(u).revoked(false)
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS)).build();
        when(refreshTokenRepository.findByToken("tok")).thenReturn(Optional.of(t));

        assertThrows(AccountDisabledException.class, () -> authService.refresh(new RefreshRequest("tok")));
    }

    @Test
    void revokedRefreshTokenIsRejected() {
        User u = User.builder().id(3L).email("a@x.com").role(Role.EMPLOYEE).active(true).build();
        RefreshToken t = RefreshToken.builder().token("tok").user(u).revoked(true)
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS)).build();
        when(refreshTokenRepository.findByToken("tok")).thenReturn(Optional.of(t));

        assertThrows(InvalidTokenException.class, () -> authService.refresh(new RefreshRequest("tok")));
    }
}
