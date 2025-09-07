package com.realm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realm.dto.LoginRequest;
import com.realm.dto.RegisterRequest;
import com.realm.model.User;
import com.realm.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AuthController REST API endpoints with Redis session-based authentication.
 * 
 * This test class validates the authentication REST endpoints including
 * registration, login, session validation, and logout.
 */
@WebMvcTest(AuthController.class)
@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private AuthService authService;
    
    @MockBean
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private User testUser;
    private MockHttpSession mockSession;
    private Authentication mockAuthentication;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("test-user-id-123")
                .email("test@example.com")
                .displayName("Test User")
                .isActive(true)
                .isVerified(true)
                .createdAt(LocalDateTime.now())
                .lastLoginAt(LocalDateTime.now())
                .build();
        
        mockSession = new MockHttpSession();
        mockAuthentication = new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
    }
    
    @Test
    void testRegisterUser_Success() throws Exception {
        when(authService.createUser(anyString(), anyString(), anyString()))
                .thenReturn(testUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuthentication);
        
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("StrongPassword123!");
        request.setDisplayName("Test User");
        
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .session(mockSession))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.user").exists())
                .andExpect(jsonPath("$.sessionId").exists())
                .andExpect(jsonPath("$.sessionTimeout").exists());
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
                .session(mockSession))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testLogin_Success() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuthentication);
        
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("StrongPassword123!");
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .session(mockSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User authenticated successfully"))
                .andExpect(jsonPath("$.user").exists())
                .andExpect(jsonPath("$.sessionId").exists())
                .andExpect(jsonPath("$.sessionTimeout").exists());
    }
    
    @Test
    void testLogin_ValidationErrors() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("invalid-email");
        request.setPassword("");
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .session(mockSession))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @WithMockUser(username = "test@example.com")
    void testValidateSession_Success() throws Exception {
        // Set up authenticated session
        mockSession.setAttribute("user", testUser);
        mockSession.setAttribute("authenticated", true);
        mockSession.setAttribute("loginTime", LocalDateTime.now());
        
        mockMvc.perform(get("/api/auth/validate")
                .session(mockSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.user").exists())
                .andExpect(jsonPath("$.sessionId").exists());
    }
    
    @Test
    void testValidateSession_NoSession() throws Exception {
        mockMvc.perform(get("/api/auth/validate"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false));
    }
    
    @Test
    void testLogout_Success() throws Exception {
        // Set up authenticated session
        mockSession.setAttribute("user", testUser);
        mockSession.setAttribute("authenticated", true);
        
        mockMvc.perform(post("/api/auth/logout")
                .session(mockSession))
                .andExpect(status().is3xxRedirection()); // Spring Security default logout behavior
    }
    
    @Test
    void testTestEndpoint() throws Exception {
        mockMvc.perform(get("/api/auth/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accessible"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
    
    @Test
    void testCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")         
                .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }
}