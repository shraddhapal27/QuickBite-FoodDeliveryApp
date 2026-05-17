package com.quickbite.restaurant.controller;

import com.quickbite.restaurant.dto.*;
import com.quickbite.restaurant.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration, login, JWT management, and profile operations")
public class AuthController {

    private final AuthService authService;

    // ─── Public Endpoints ─────────────────────────────────────────────────────

    @Operation(summary = "Register a new user", description = "Creates a new user account and returns JWT tokens")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registration successful"),
            @ApiResponse(responseCode = "400", description = "Invalid input or email already registered")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @Operation(summary = "Login", description = "Authenticates a user and returns JWT access + refresh tokens")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Refresh token", description = "Exchanges a valid refresh token for a new access token pair")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @Operation(summary = "Forgot password", description = "Sends a password reset link to the user's email if it exists")
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(Map.of("message",
                "If that email exists, a reset link has been sent."));
    }

    @Operation(summary = "Reset password", description = "Resets the password using a valid reset token")
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully."));
    }

    // ─── Authenticated Endpoints ───────────────────────────────────────────────

    @Operation(summary = "Logout", description = "Invalidates the refresh token and blacklists the access token")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestBody RefreshTokenRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }
        authService.logout(request.getRefreshToken(), accessToken);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }

    @Operation(summary = "Get profile", description = "Returns the current authenticated user's profile")
    @GetMapping("/profile")
    public ResponseEntity<java.util.Map<String, Object>> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        AuthResponse auth = authService.getUserByEmail(userDetails.getUsername());
        java.util.Map<String, Object> profile = new java.util.LinkedHashMap<>();
        profile.put("id", auth.getUserId());
        profile.put("userId", auth.getUserId());
        profile.put("fullName", auth.getFullName());
        profile.put("email", auth.getEmail());
        profile.put("role", auth.getRole());
        profile.put("emailVerified", auth.isEmailVerified());
        return ResponseEntity.ok(profile);
    }

    @Operation(summary = "Update profile", description = "Updates the authenticated user's profile fields")
    @PutMapping("/profile")
    public ResponseEntity<AuthResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(authService.updateProfile(userDetails.getUsername(), request));
    }

    @Operation(summary = "Change password", description = "Changes the password for the authenticated user")
    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }

    @Operation(summary = "Deactivate account", description = "Soft-deletes the authenticated user's account")
    @DeleteMapping("/deactivate")
    public ResponseEntity<Map<String, String>> deactivateAccount(
            @AuthenticationPrincipal UserDetails userDetails) {
        authService.deactivateAccount(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Account deactivated successfully."));
    }

    @Operation(summary = "Get user by ID", description = "Retrieves a user's public profile by their user ID")
    @GetMapping("/user/{userId}")
    public ResponseEntity<AuthResponse> getUserById(
            @Parameter(description = "User ID") @PathVariable String userId) {
        return ResponseEntity.ok(authService.getUserById(userId));
    }

    @Operation(summary = "Get all users (Admin)", description = "Admin endpoint to list all registered users")
    @GetMapping("/admin/users")
    public ResponseEntity<java.util.List<AuthResponse>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }
}
