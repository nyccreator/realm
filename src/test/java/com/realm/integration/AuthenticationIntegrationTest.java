package com.realm.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realm.dto.LoginRequest;
import com.realm.dto.RegisterRequest;
import com.realm.model.User;
import com.realm.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import com.realm.config.Neo4jTestConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the authentication system.
 * 
 * Tests the complete authentication flow including:
 * - User registration
 * - User login
 * - Token validation
 * - Token refresh
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(Neo4jTestConfig.class)
@Transactional
public class AuthenticationIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @AfterEach
    void cleanup() {
        // Clean up test data
        userRepository.deleteAll();
    }
    
    @Test
    void testUserRegistration_Success() throws Exception {
        RegisterRequest request = new RegisterRequest(
            "test@example.com",
            "TestPassword123!",
            "Test User"
        );
        
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user").exists())
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.user.displayName").value("Test User"));
    }
    
    @Test
    void testUserRegistration_ValidationError() throws Exception {
        RegisterRequest request = new RegisterRequest(
            "invalid-email",
            "weak",
            ""
        );
        
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors").exists());
    }
    
    @Test
    void testUserLogin_Success() throws Exception {
        // First register a user
        RegisterRequest registerRequest = new RegisterRequest(
            "login@example.com",
            "TestPassword123!",
            "Login User"
        );
        
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());
        
        // Now try to login
        LoginRequest loginRequest = new LoginRequest(
            "login@example.com",
            "TestPassword123!"
        );
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user").exists())
                .andExpect(jsonPath("$.user.email").value("login@example.com"));
    }
    
    @Test
    void testUserLogin_InvalidCredentials() throws Exception {
        LoginRequest loginRequest = new LoginRequest(
            "nonexistent@example.com",
            "WrongPassword123!"
        );
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication Failed"));
    }
    
    @Test
    void testTokenValidation_NoToken() throws Exception {
        mockMvc.perform(get("/api/auth/validate"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.error").value("Invalid Token"))
                .andExpect(jsonPath("$.message").value("Authorization header missing or invalid format"));
    }
    
    @Test
    void testTokenValidation_InvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/validate")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.error").value("Invalid Token"));
    }
    
    @Test
    void testTokenRefresh_NoRefreshToken() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Request"))
                .andExpect(jsonPath("$.message").value("Refresh token is required"));
    }
    
    @Test
    void testTokenRefresh_InvalidRefreshToken() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\": \"invalid-refresh-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Token Refresh Failed"));
    }
    
    @Test
    @SuppressWarnings("unchecked")
    void testCompleteAuthenticationFlow() throws Exception {
        // 1. Register a user
        RegisterRequest registerRequest = new RegisterRequest(
            "flow@example.com",
            "TestPassword123!",
            "Flow User"
        );
        
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        
        Map<String, Object> registerResponse = objectMapper.readValue(
            registerResult.getResponse().getContentAsString(), Map.class);
        String accessToken = (String) registerResponse.get("accessToken");
        String refreshToken = (String) registerResponse.get("refreshToken");
        
        // 2. Validate the access token
        mockMvc.perform(get("/api/auth/validate")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.user").exists())
                .andExpect(jsonPath("$.user.email").value("flow@example.com"));
        
        // 3. Use refresh token to get new access token
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user").exists());
        
        // 4. Login with the same credentials
        LoginRequest loginRequest = new LoginRequest(
            "flow@example.com",
            "TestPassword123!"
        );
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.user.email").value("flow@example.com"));
    }
}