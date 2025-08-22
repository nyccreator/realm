package com.realm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realm.model.User;
import com.realm.service.AuthService;
import com.realm.service.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AuthController REST API endpoints.
 * 
 * This test class validates all authentication REST endpoints including
 * registration, login, token refresh, profile management, and security
 * handling. It uses MockMvc for web layer testing and mocks the service
 * layer to focus on controller behavior.
 */
@WebMvcTest(AuthController.class)
@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private AuthService authService;
    
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private User testUser;
    private AuthService.AuthResult successAuthResult;
    private AuthService.AuthResult failureAuthResult;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("test-user-id")
                .email("test@example.com")
                .displayName("Test User")
                .firstName("Test")
                .lastName("User")
                .isActive(true)
                .isVerified(true)
                .createdAt(LocalDateTime.now())
                .lastLoginAt(LocalDateTime.now())
                .build();
        
        successAuthResult = AuthService.AuthResult.success(
                "Operation successful",
                "access-token-123",
                "refresh-token-456",
                testUser
        );
        
        failureAuthResult = AuthService.AuthResult.failure("Operation failed");
    }
    
    @Test
    void testRegisterUser_Success() throws Exception {
        when(authService.registerUser(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(successAuthResult);
        
        AuthController.RegisterRequest request = new AuthController.RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("StrongPassword123!");
        request.setDisplayName("Test User");
        request.setFirstName("Test");
        request.setLastName("User");
        
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token-456"))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"));
    }
    
    @Test
    void testRegisterUser_Failure() throws Exception {
        when(authService.registerUser(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(failureAuthResult);
        
        AuthController.RegisterRequest request = new AuthController.RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("weak");
        request.setDisplayName("Test User");
        
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Operation failed"));
    }
    
    @Test
    void testRegisterUser_ValidationErrors() throws Exception {
        AuthController.RegisterRequest request = new AuthController.RegisterRequest();
        request.setEmail("invalid-email");
        request.setPassword("short");
        request.setDisplayName("");
        
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testLogin_Success() throws Exception {
        when(authService.authenticateUser(anyString(), anyString()))
                .thenReturn(successAuthResult);
        
        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("StrongPassword123!");
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token-456"));
    }
    
    @Test
    void testLogin_Failure() throws Exception {
        when(authService.authenticateUser(anyString(), anyString()))
                .thenReturn(failureAuthResult);
        
        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Operation failed"));
    }
    
    @Test
    void testRefreshToken_Success() throws Exception {
        when(authService.refreshToken(anyString()))
                .thenReturn(successAuthResult);
        
        AuthController.RefreshTokenRequest request = new AuthController.RefreshTokenRequest();
        request.setRefreshToken("refresh-token-456");
        
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"));
    }
    
    @Test
    @WithMockUser(username = "test@example.com")
    void testGetCurrentUser_Success() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User profile retrieved"));
    }
    
    @Test
    void testGetCurrentUser_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    @WithMockUser(username = "test@example.com")
    void testUpdateProfile_Success() throws Exception {
        when(authService.updateProfile(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(successAuthResult);
        
        AuthController.UpdateProfileRequest request = new AuthController.UpdateProfileRequest();
        request.setDisplayName("Updated Name");
        request.setFirstName("Updated");
        request.setLastName("User");
        request.setBio("Updated bio");
        
        mockMvc.perform(put("/api/auth/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Profile updated successfully"));
    }
    
    @Test
    @WithMockUser(username = "test@example.com")
    void testChangePassword_Success() throws Exception {
        when(authService.changePassword(anyString(), anyString(), anyString()))
                .thenReturn(successAuthResult);
        
        AuthController.ChangePasswordRequest request = new AuthController.ChangePasswordRequest();
        request.setCurrentPassword("OldPassword123!");
        request.setNewPassword("NewPassword123!");
        
        mockMvc.perform(put("/api/auth/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
    }
    
    @Test
    void testVerifyAccount_Success() throws Exception {
        when(authService.verifyUser(anyString()))
                .thenReturn(successAuthResult);
        
        AuthController.VerifyAccountRequest request = new AuthController.VerifyAccountRequest();
        request.setUserId("test-user-id");
        
        mockMvc.perform(post("/api/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account verified successfully"));
    }
    
    @Test
    @WithMockUser(username = "test@example.com")
    void testLogout_Success() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful"));
    }
    
    @Test
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/auth/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Authentication service is healthy"))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }
    
    @Test
    void testInvalidEndpoint() throws Exception {
        mockMvc.perform(get("/api/auth/nonexistent"))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void testCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().exists("Access-Control-Allow-Methods"))
                .andExpect(header().exists("Access-Control-Allow-Headers"));
    }
}