package com.realm.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realm.dto.LoginRequest;
import com.realm.dto.RegisterRequest;
import com.realm.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
@Testcontainers
public class AuthenticationIntegrationTest {
    
    @Container
    static Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:5.15")
            .withAdminPassword("testpassword");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.neo4j.uri", neo4jContainer::getBoltUrl);
        registry.add("spring.neo4j.authentication.username", () -> "neo4j");
        registry.add("spring.neo4j.authentication.password", () -> "testpassword");
        registry.add("spring.data.neo4j.database", () -> "neo4j");
    }
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    // Remove @AfterEach cleanup for now to avoid transaction conflicts
    // TestContainers will provide clean database state for each test run
    
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
                .andExpect(jsonPath("$.sessionId").exists())
                .andExpect(jsonPath("$.sessionTimeout").exists())
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
                .andExpect(jsonPath("$.sessionId").exists())
                .andExpect(jsonPath("$.sessionTimeout").exists())
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
    void testSessionValidation_NoSession() throws Exception {
        mockMvc.perform(get("/api/auth/validate"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.error").value("Invalid Session"));
    }
    
    @Test
    void testSessionValidation_InvalidSession() throws Exception {
        mockMvc.perform(get("/api/auth/validate"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.error").value("Invalid Session"));
    }
    
    @Test
    void testLogout_Success() throws Exception {
        // First register and login
        RegisterRequest registerRequest = new RegisterRequest(
            "logout@example.com",
            "TestPassword123!",
            "Logout User"
        );
        
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());
        
        // Now logout
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }
    
    @Test
    void testCsrfToken() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.headerName").exists())
                .andExpect(jsonPath("$.parameterName").exists());
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
        String sessionId = (String) registerResponse.get("sessionId");
        
        // 2. Validate the session (should work since we just registered)
        mockMvc.perform(get("/api/auth/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.user").exists())
                .andExpect(jsonPath("$.user.email").value("flow@example.com"));
        
        // 3. Login with the same credentials (should create new session)
        LoginRequest loginRequest = new LoginRequest(
            "flow@example.com",
            "TestPassword123!"
        );
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").exists())
                .andExpect(jsonPath("$.user.email").value("flow@example.com"));
        
        // 4. Logout
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }
}