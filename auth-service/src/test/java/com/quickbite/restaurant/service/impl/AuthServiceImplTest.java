	package com.quickbite.restaurant.service.impl;

import com.quickbite.restaurant.dto.*;
import com.quickbite.restaurant.entity.User;
import com.quickbite.restaurant.enums.AuthProvider;
import com.quickbite.restaurant.enums.Role;
import com.quickbite.restaurant.repository.UserRepository;
import com.quickbite.restaurant.security.CustomUserDetails;
import com.quickbite.restaurant.security.JwtService;
import com.quickbite.restaurant.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Unit Tests")
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private EmailService emailService;

    @InjectMocks private AuthServiceImpl authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L).fullName("John Doe").email("john@example.com")
                .passwordHash("hashedPwd").role(Role.CUSTOMER)
                .provider(AuthProvider.LOCAL).phone("9876543210")
                .isActive(true).createdAt(LocalDateTime.now())
                .refreshToken("old-refresh-token").build();
    }

    // ── Register ──

    @Test
    @DisplayName("register – success with default CUSTOMER role")
    void register_success() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Jane"); req.setEmail("jane@test.com"); req.setPassword("pass");

        when(userRepository.existsByEmail("jane@test.com")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(i -> { User u = i.getArgument(0); u.setId(2L); return u; });
        when(jwtService.generateToken(any(CustomUserDetails.class))).thenReturn("jwt");

        AuthResponse res = authService.register(req);
        assertThat(res.getAccessToken()).isEqualTo("jwt");
        assertThat(res.getRole()).isEqualTo("CUSTOMER");
        assertThat(res.getTokenType()).isEqualTo("Bearer");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register – duplicate email throws")
    void register_duplicate() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("john@example.com"); req.setFullName("J"); req.setPassword("p");
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);
        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(RuntimeException.class).hasMessage("Email is already registered");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register – password is encoded before save")
    void register_encodesPassword() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("J"); req.setEmail("j@t.com"); req.setPassword("raw");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("raw")).thenReturn("$2a$enc");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("t");
        authService.register(req);
        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(cap.capture());
        assertThat(cap.getValue().getPasswordHash()).isEqualTo("$2a$enc");
    }

    // ── Login ──

    @Test
    @DisplayName("login – success")
    void login_success() {
        LoginRequest req = new LoginRequest();
        req.setEmail("john@example.com"); req.setPassword("pass");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(any(CustomUserDetails.class))).thenReturn("access");
        AuthResponse res = authService.login(req);
        assertThat(res.getAccessToken()).isEqualTo("access");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("login – user not found throws")
    void login_notFound() {
        LoginRequest req = new LoginRequest();
        req.setEmail("x@x.com"); req.setPassword("p");
        when(userRepository.findByEmail("x@x.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login(req)).hasMessage("User not found");
    }

    @Test
    @DisplayName("login – deactivated account throws")
    void login_deactivated() {
        testUser.setActive(false);
        LoginRequest req = new LoginRequest();
        req.setEmail("john@example.com"); req.setPassword("p");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        assertThatThrownBy(() -> authService.login(req))
                .hasMessage("Account is deactivated. Please contact support.");
    }

    // ── Logout ──
//
//    @Test
//    @DisplayName("logout – clears refresh token")
//    void logout_success() {
//        when(userRepository.findByRefreshToken("old-refresh-token")).thenReturn(Optional.of(testUser));
//        authService.logout("old-refresh-token");
//        assertThat(testUser.getRefreshToken()).isNull();
//        verify(userRepository).save(testUser);
//    }
//
//    @Test
//    @DisplayName("logout – unknown token is silent")
//    void logout_unknown() {
//        when(userRepository.findByRefreshToken("x")).thenReturn(Optional.empty());
//        assertThatNoException().isThrownBy(() -> authService.logout("x"));
//        verify(userRepository, never()).save(any());
//    }

    // ── Refresh Token ──

    @Test
    @DisplayName("refreshToken – success returns new tokens")
    void refreshToken_success() {
        RefreshTokenRequest req = new RefreshTokenRequest(); req.setRefreshToken("old-refresh-token");
        when(userRepository.findByRefreshToken("old-refresh-token")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(any())).thenReturn("new-access");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        AuthResponse res = authService.refreshToken(req);
        assertThat(res.getAccessToken()).isEqualTo("new-access");
        assertThat(res.getRefreshToken()).isNotEqualTo("old-refresh-token");
    }

    @Test
    @DisplayName("refreshToken – invalid token throws")
    void refreshToken_invalid() {
        RefreshTokenRequest req = new RefreshTokenRequest(); req.setRefreshToken("bad");
        when(userRepository.findByRefreshToken("bad")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.refreshToken(req)).hasMessage("Invalid or expired refresh token");
    }

    // ── Profile ──

    @Test
    @DisplayName("getUserByEmail – success")
    void getUserByEmail_success() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        AuthResponse res = authService.getUserByEmail("john@example.com");
        assertThat(res.getEmail()).isEqualTo("john@example.com");
        assertThat(res.getAccessToken()).isNull();
    }

    @Test
    @DisplayName("getUserById – success")
    void getUserById_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        AuthResponse res = authService.getUserById("1");
        assertThat(res.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("updateProfile – partial update")
    void updateProfile_partial() {
        UpdateProfileRequest req = new UpdateProfileRequest(); req.setFullName("Updated");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        authService.updateProfile("john@example.com", req);
        assertThat(testUser.getFullName()).isEqualTo("Updated");
        assertThat(testUser.getPhone()).isEqualTo("9876543210");
    }

    // ── Change Password ──

    @Test
    @DisplayName("changePassword – success")
    void changePassword_success() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("old"); req.setNewPassword("new123");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("old", "hashedPwd")).thenReturn(true);
        when(passwordEncoder.encode("new123")).thenReturn("newHash");
        authService.changePassword("john@example.com", req);
        assertThat(testUser.getPasswordHash()).isEqualTo("newHash");
        assertThat(testUser.getRefreshToken()).isNull();
    }

    @Test
    @DisplayName("changePassword – OAuth user rejected")
    void changePassword_oauthReject() {
        testUser.setProvider(AuthProvider.GOOGLE);
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("x"); req.setNewPassword("y");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        assertThatThrownBy(() -> authService.changePassword("john@example.com", req))
                .hasMessage("Cannot change password for Google/OAuth accounts");
    }

    @Test
    @DisplayName("changePassword – wrong current password rejected")
    void changePassword_wrongCurrent() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("wrong"); req.setNewPassword("new");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", "hashedPwd")).thenReturn(false);
        assertThatThrownBy(() -> authService.changePassword("john@example.com", req))
                .hasMessage("Current password is incorrect");
    }

    // ── Forgot / Reset Password ──

    @Test
    @DisplayName("forgotPassword – sends email for LOCAL user")
    void forgotPassword_local() {
        ForgotPasswordRequest req = new ForgotPasswordRequest(); req.setEmail("john@example.com");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        authService.forgotPassword(req);
        verify(emailService).sendPasswordResetEmail(eq("john@example.com"), anyString());
        assertThat(testUser.getPasswordResetToken()).isNotNull();
    }

    @Test
    @DisplayName("forgotPassword – silent for unknown email")
    void forgotPassword_unknown() {
        ForgotPasswordRequest req = new ForgotPasswordRequest(); req.setEmail("x@x.com");
        when(userRepository.findByEmail("x@x.com")).thenReturn(Optional.empty());
        assertThatNoException().isThrownBy(() -> authService.forgotPassword(req));
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("resetPassword – success with valid token")
    void resetPassword_success() {
        testUser.setPasswordResetToken("tok");
        testUser.setPasswordResetTokenExpiry(LocalDateTime.now().plusMinutes(10));
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("tok"); req.setNewPassword("newPwd");
        when(userRepository.findByPasswordResetToken("tok")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPwd")).thenReturn("encNew");
        authService.resetPassword(req);
        assertThat(testUser.getPasswordHash()).isEqualTo("encNew");
        assertThat(testUser.getPasswordResetToken()).isNull();
    }

    @Test
    @DisplayName("resetPassword – expired token throws")
    void resetPassword_expired() {
        testUser.setPasswordResetToken("tok");
        testUser.setPasswordResetTokenExpiry(LocalDateTime.now().minusMinutes(5));
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("tok"); req.setNewPassword("p");
        when(userRepository.findByPasswordResetToken("tok")).thenReturn(Optional.of(testUser));
        assertThatThrownBy(() -> authService.resetPassword(req)).hasMessageContaining("expired");
    }

    // ── Deactivate ──

    @Test
    @DisplayName("deactivateAccount – success")
    void deactivate_success() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        authService.deactivateAccount("john@example.com");
        assertThat(testUser.isActive()).isFalse();
        assertThat(testUser.getRefreshToken()).isNull();
    }

    // ── Get All Users ──

    @Test
    @DisplayName("getAllUsers – returns mapped list")
    void getAllUsers() {
        User u2 = User.builder().id(2L).fullName("Jane").email("jane@t.com")
                .role(Role.OWNER).provider(AuthProvider.LOCAL).isActive(true).build();
        when(userRepository.findAll()).thenReturn(List.of(testUser, u2));
        List<AuthResponse> list = authService.getAllUsers();
        assertThat(list).hasSize(2);
        assertThat(list.get(1).getRole()).isEqualTo("OWNER");
    }
}
