package com.example.expense.service;

import com.example.expense.dto.*;
import com.example.expense.entity.RefreshToken;
import com.example.expense.entity.Role;
import com.example.expense.entity.User;
import com.example.expense.exception.AccountDisabledException;
import com.example.expense.exception.BusinessRuleException;
import com.example.expense.exception.InvalidTokenException;
import com.example.expense.repository.RefreshTokenRepository;
import com.example.expense.repository.UserRepository;
import com.example.expense.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /** Public registration ALWAYS creates an EMPLOYEE; the role is never taken from the request. */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException("Email is already registered");
        }
        User user = User.builder()
                .name(req.name().trim())
                .email(email)
                .password(passwordEncoder.encode(req.password()))
                .role(Role.EMPLOYEE)
                .active(true)
                .build();
        user = userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmailIgnoreCase(req.email().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        if (!user.isActive()) {
            throw new AccountDisabledException("Your account has been deactivated. Contact your manager.");
        }
        return issueTokens(user);
    }

    /** Rotates the refresh token: the old one is revoked and a new pair is returned. */
    @Transactional
    public AuthResponse refresh(RefreshRequest req) {
        RefreshToken stored = refreshTokenRepository.findByToken(req.refreshToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token expired or revoked");
        }
        User user = stored.getUser();
        if (!user.isActive()) {
            stored.setRevoked(true);
            throw new AccountDisabledException("Your account has been deactivated. Contact your manager.");
        }
        stored.setRevoked(true);
        return issueTokens(user);
    }

    @Transactional
    public void logout(RefreshRequest req) {
        refreshTokenRepository.findByToken(req.refreshToken()).ifPresent(t -> t.setRevoked(true));
    }

    private AuthResponse issueTokens(User user) {
        RefreshToken refresh = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(jwtService.refreshTokenExpiry())
                .revoked(false)
                .build();
        refreshTokenRepository.save(refresh);
        return new AuthResponse(jwtService.generateAccessToken(user), refresh.getToken(), UserResponse.from(user));
    }
}
