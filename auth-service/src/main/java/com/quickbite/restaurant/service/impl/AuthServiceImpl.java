package com.quickbite.restaurant.service.impl;

import com.quickbite.restaurant.dto.*;
import com.quickbite.restaurant.entity.User;
import com.quickbite.restaurant.enums.AuthProvider;
import com.quickbite.restaurant.enums.Role;
import com.quickbite.restaurant.repository.UserRepository;
import com.quickbite.restaurant.security.CustomUserDetails;
import com.quickbite.restaurant.security.JwtService;
import com.quickbite.restaurant.service.AuthService;
import com.quickbite.restaurant.service.EmailService;
import com.quickbite.restaurant.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final TokenBlacklistService tokenBlacklistService;

    // ─── Register ────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.getEmail());
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed — email already exists: {}", request.getEmail());
            throw new RuntimeException("Email is already registered");
        }
        Role role = (request.getRole() != null) ? request.getRole() : Role.CUSTOMER;
        String refreshToken = UUID.randomUUID().toString();

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .provider(AuthProvider.LOCAL)
                .phone(request.getPhone())
                .isActive(true)
                .refreshToken(refreshToken)
                .build();

        userRepository.save(user);
        log.info("User registered successfully: {} (role={})", user.getEmail(), role);
        String accessToken = jwtService.generateToken(new CustomUserDetails(user));
        return buildAuthResponse(accessToken, refreshToken, user);
    }

    // ─── Login ───────────────────────────────────────────────────────────────
    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!user.isActive()) {
            log.warn("Login blocked — account deactivated: {}", request.getEmail());
            throw new RuntimeException("Account is deactivated. Please contact support.");
        }
        String refreshToken = UUID.randomUUID().toString();
        user.setRefreshToken(refreshToken);
        userRepository.save(user);
        log.info("Login successful: {} (role={})", user.getEmail(), user.getRole());
        String accessToken = jwtService.generateToken(new CustomUserDetails(user));
        return buildAuthResponse(accessToken, refreshToken, user);
    }

    // ─── Logout ──────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void logout(String refreshToken, String accessToken) {
        // Blacklist the access token in Redis so it cannot be reused
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                long expirationMillis = jwtService.getExpirationMillis(accessToken);
                tokenBlacklistService.blacklistToken(accessToken, expirationMillis);
            } catch (Exception e) {
                log.warn("Could not blacklist access token: {}", e.getMessage());
            }
        }
        userRepository.findByRefreshToken(refreshToken).ifPresent(user -> {
            log.info("User logged out: {}", user.getEmail());
            user.setRefreshToken(null);
            userRepository.save(user);
        });
    }

    // ─── Refresh Token ───────────────────────────────────────────────────────
    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        User user = userRepository.findByRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Invalid or expired refresh token"));
        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated");
        }
        String newRefreshToken = UUID.randomUUID().toString();
        user.setRefreshToken(newRefreshToken);
        userRepository.save(user);
        String accessToken = jwtService.generateToken(new CustomUserDetails(user));
        return buildAuthResponse(accessToken, newRefreshToken, user);
    }

    // ─── OAuth2 ──────────────────────────────────────────────────────────────
    @Override
    public AuthResponse processOAuth2Login(String email, String fullName, String profilePicUrl) {
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .fullName(fullName)
                    .email(email)
                    .role(Role.CUSTOMER)
                    .provider(AuthProvider.GOOGLE)
                    .profilePicUrl(profilePicUrl)
                    .isActive(true)
                    .refreshToken(UUID.randomUUID().toString())
                    .build();
            return userRepository.save(newUser);
        });
        String refreshToken = UUID.randomUUID().toString();
        user.setRefreshToken(refreshToken);
        user.setProfilePicUrl(profilePicUrl);
        userRepository.save(user);
        String accessToken = jwtService.generateToken(new CustomUserDetails(user));
        return buildAuthResponse(accessToken, refreshToken, user);
    }

    // ─── Get Profile ─────────────────────────────────────────────────────────
    @Override
    public AuthResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return buildProfileResponse(user);
    }

    @Override
    public AuthResponse getUserById(String userId) {
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));
        return buildProfileResponse(user);
    }

    // ─── Update Profile ───────────────────────────────────────────────────────
    @Override
    public AuthResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getProfilePicUrl() != null) {
            user.setProfilePicUrl(request.getProfilePicUrl());
        }
        userRepository.save(user);
        return buildProfileResponse(user);
    }

    // ─── Change Password ──────────────────────────────────────────────────────
    @Override
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new RuntimeException("Cannot change password for Google/OAuth accounts");
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setRefreshToken(null); // Invalidate all sessions after password change
        userRepository.save(user);
    }

    // ─── Deactivate Account ───────────────────────────────────────────────────
    @Override
    @Transactional
    public void deactivateAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(false);
        user.setRefreshToken(null);
        userRepository.save(user);
    }

    // ─── Forgot Password ──────────────────────────────────────────────────────
    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        // Silently return if email not found (prevents email enumeration attacks)
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            if (user.getProvider() != AuthProvider.LOCAL) {
                // Email belongs to OAuth user — still silent, or could send notification
                return;
            }
            String resetToken = UUID.randomUUID().toString();
            user.setPasswordResetToken(resetToken);
            user.setPasswordResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
            userRepository.save(user);
            emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
        });
    }

    // ─── Reset Password ───────────────────────────────────────────────────────
    @Override
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));
        if (user.getPasswordResetTokenExpiry() == null
                || LocalDateTime.now().isAfter(user.getPasswordResetTokenExpiry())) {
            throw new RuntimeException("Reset token has expired. Please request a new one.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        user.setRefreshToken(null); // Invalidate all sessions
        userRepository.save(user);
    }

    @Override
    public java.util.List<AuthResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::buildProfileResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private AuthResponse buildAuthResponse(String accessToken, String refreshToken, User user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .userId(user.getId())
                .emailVerified(true)
                .build();
    }

    private AuthResponse buildProfileResponse(User user) {
        return AuthResponse.builder()
                .accessToken(null)
                .refreshToken(null)
                .tokenType(null)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .userId(user.getId())
                .emailVerified(true)
                .build();
    }
}
