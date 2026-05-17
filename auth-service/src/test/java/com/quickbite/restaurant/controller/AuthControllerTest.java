package com.quickbite.restaurant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickbite.restaurant.dto.*;
import com.quickbite.restaurant.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Unit Tests")
class AuthControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    private AuthResponse sampleResponse() {
        return AuthResponse.builder()
                .accessToken("jwt-token").refreshToken("refresh-token")
                .tokenType("Bearer").email("john@test.com")
                .fullName("John Doe").role("CUSTOMER").userId(1L).emailVerified(true)
                .build();
    }

    @Test
    @DisplayName("POST /auth/register – 200 OK")
    void register_success() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("John"); req.setEmail("john@test.com"); req.setPassword("pass123");
        when(authService.register(any(RegisterRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("john@test.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("POST /auth/login – 200 OK")
    void login_success() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("john@test.com"); req.setPassword("pass");
        when(authService.login(any(LoginRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"));
    }

    @Test
    @DisplayName("POST /auth/refresh – 200 OK")
    void refresh_success() throws Exception {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("ref-tok");
        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    @DisplayName("POST /auth/forgot-password – 200 OK with message")
    void forgotPassword() throws Exception {
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("john@test.com");
        doNothing().when(authService).forgotPassword(any());

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("reset link")));
    }

    @Test
    @DisplayName("POST /auth/reset-password – 200 OK with message")
    void resetPassword() throws Exception {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("tok"); req.setNewPassword("newPass123");
        doNothing().when(authService).resetPassword(any());

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully."));
    }

    @Test
    @DisplayName("GET /auth/user/{userId} – 200 OK")
    void getUserById() throws Exception {
        when(authService.getUserById("1")).thenReturn(sampleResponse());

        mockMvc.perform(get("/auth/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    @DisplayName("GET /auth/admin/users – returns list")
    void getAllUsers() throws Exception {
        when(authService.getAllUsers()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/auth/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}
