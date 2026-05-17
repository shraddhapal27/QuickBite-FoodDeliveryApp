package com.quickbite.restaurant.service;

import com.quickbite.restaurant.dto.*;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    void logout(String refreshToken, String accessToken);
    AuthResponse refreshToken(RefreshTokenRequest request);
    AuthResponse processOAuth2Login(String email, String fullName, String profilePicUrl);
    AuthResponse getUserByEmail(String email);
    AuthResponse getUserById(String userId);
    AuthResponse updateProfile(String email, UpdateProfileRequest request);
    void changePassword(String email, ChangePasswordRequest request);
    void deactivateAccount(String email);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
    java.util.List<AuthResponse> getAllUsers();
}
