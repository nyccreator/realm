package com.realm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realm.dto.LoginRequest;
import com.realm.dto.RegisterRequest;
import com.realm.model.User;
import com.realm.service.AuthService;
import com.realm.service.AuthenticationService;
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
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AuthController REST API endpoints.
 * 
 * This test class validates the authentication REST endpoints including
 * registration, login, token validation, and token refresh.
 */
@WebMvcTest(AuthController.class)
@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private AuthService authService;
    
    @MockBean
    private AuthenticationService authenticationService;
    
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
        
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("StrongPassword123!");
        request.setDisplayName("Test User");
        
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-456"));
    }
    
    @Test
    void testRegisterUser_ValidationErrors() throws Exception {
        RegisterRequest request = new RegisterRequest();
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
        
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("StrongPassword123!");
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-456"));
    }
    
    @Test
    void testLogin_ValidationErrors() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("invalid-email");
        request.setPassword("");
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testRefreshToken_Success() throws Exception {
        when(authService.refreshToken(anyString()))
                .thenReturn(successAuthResult);
        
        Map<String, String> request = new HashMap<>();
        request.put("refreshToken", "refresh-token-456");
        
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"));
    }
    
    @Test
    void testValidateToken_Success() throws Exception {
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getExpirationDateFromToken(anyString()))
                .thenReturn(new java.util.Date(System.currentTimeMillis() + 3600000));
        when(jwtTokenProvider.getRemainingTimeToExpire(anyString())).thenReturn(3600000L);
        
        mockMvc.perform(get("/api/auth/validate")
                .header("Authorization", "Bearer valid-token-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }
    
    @Test
    void testValidateToken_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/validate"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false));
    }
    
    @Test
    void testCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")         
                .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }
}