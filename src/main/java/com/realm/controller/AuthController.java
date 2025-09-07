package com.realm.controller;

import com.realm.dto.LoginRequest;
import com.realm.dto.RegisterRequest;
import com.realm.exception.UserAlreadyExistsException;
import com.realm.exception.ValidationException;
import com.realm.model.User;
import com.realm.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Authentication REST API Controller for Redis Session-Based Authentication
 * 
 * This controller provides secure REST endpoints for session-based authentication:
 * - POST /api/auth/register - User registration with session creation
 * - POST /api/auth/login - User authentication with session management  
 * - GET /api/auth/validate - Session validation
 * - POST /api/auth/logout - Session invalidation
 * - GET /api/auth/csrf - CSRF token retrieval
 * 
 * Features:
 * - Redis-backed session storage
 * - CSRF protection support
 * - Comprehensive input validation
 * - Structured error responses
 * - Security-focused logging
 * - HTTP-only session cookies
 * - CORS-enabled with credentials support
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@Slf4j
public class AuthController {
    
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    
    @Autowired
    public AuthController(AuthService authService, AuthenticationManager authenticationManager) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
    }
    
    /**
     * Register a new user and create session
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, 
                                     BindingResult bindingResult,
                                     HttpServletRequest httpRequest) {
        log.info("Registration attempt for email: {}", request.getEmail());
        
        try {
            // Check for validation errors
            if (bindingResult.hasErrors()) {
                Map<String, Object> errorResponse = createValidationErrorResponse(bindingResult);
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Register user (without JWT tokens)
            User user = authService.createUser(
                request.getEmail(),
                request.getPassword(),
                request.getDisplayName()
            );
            
            // Authenticate the newly registered user and create session
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            
            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Create HTTP session
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("user", user);
            session.setAttribute("authenticated", true);
            session.setAttribute("loginTime", LocalDateTime.now());
            
            // Create success response
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("user", user);
            response.put("sessionId", session.getId());
            response.put("sessionTimeout", session.getMaxInactiveInterval());
            
            log.info("User registered and session created successfully: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (UserAlreadyExistsException e) {
            log.warn("Registration failed - user already exists: {}", request.getEmail());
            Map<String, Object> errorResponse = createErrorResponse(
                "Registration Failed", 
                e.getMessage(), 
                HttpStatus.CONFLICT.value()
            );
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            
        } catch (ValidationException e) {
            log.warn("Registration failed - validation error: {}", e.getMessage());
            Map<String, Object> errorResponse = createErrorResponse(
                "Validation Error", 
                e.getMessage(), 
                HttpStatus.BAD_REQUEST.value()
            );
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            log.error("Unexpected error during registration for {}: {}", request.getEmail(), e.getMessage(), e);
            Map<String, Object> errorResponse = createErrorResponse(
                "Registration Failed", 
                "An unexpected error occurred during registration", 
                HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Authenticate user and create session
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, 
                                  BindingResult bindingResult,
                                  HttpServletRequest httpRequest) {
        log.info("Login attempt for email: {}", request.getEmail());
        
        try {
            // Check for validation errors
            if (bindingResult.hasErrors()) {
                Map<String, Object> errorResponse = createValidationErrorResponse(bindingResult);
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            
            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Get authenticated user
            User user = (User) authentication.getPrincipal();
            
            // Create HTTP session
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("user", user);
            session.setAttribute("authenticated", true);
            session.setAttribute("loginTime", LocalDateTime.now());
            
            // Create success response
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User authenticated successfully");
            response.put("user", user);
            response.put("sessionId", session.getId());
            response.put("sessionTimeout", session.getMaxInactiveInterval());
            
            log.info("User authenticated and session created successfully: {}", request.getEmail());
            return ResponseEntity.ok(response);
            
        } catch (BadCredentialsException e) {
            log.warn("Authentication failed for {}: Invalid credentials", request.getEmail());
            Map<String, Object> errorResponse = createErrorResponse(
                "Authentication Failed", 
                "Invalid email or password", 
                HttpStatus.UNAUTHORIZED.value()
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            
        } catch (org.springframework.security.core.AuthenticationException e) {
            log.warn("Authentication failed for {}: {}", request.getEmail(), e.getMessage());
            Map<String, Object> errorResponse = createErrorResponse(
                "Authentication Failed", 
                "Authentication failed", 
                HttpStatus.UNAUTHORIZED.value()
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            
        } catch (ValidationException e) {
            log.warn("Login failed - validation error: {}", e.getMessage());
            Map<String, Object> errorResponse = createErrorResponse(
                "Validation Error", 
                e.getMessage(), 
                HttpStatus.BAD_REQUEST.value()
            );
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            log.error("Unexpected error during login for {}: {}", request.getEmail(), e.getMessage(), e);
            Map<String, Object> errorResponse = createErrorResponse(
                "Authentication Failed", 
                "An unexpected error occurred during authentication", 
                HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Simple test endpoint to debug authentication issues
     * GET /api/auth/test
     */
    @GetMapping("/test")
    public ResponseEntity<?> testEndpoint(HttpServletRequest httpRequest) {
        log.info("Test endpoint accessed");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "accessible");
        response.put("timestamp", LocalDateTime.now());
        
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            response.put("sessionId", session.getId());
            // Convert session attribute names to a list for JSON serialization
            List<String> attributeNames = new ArrayList<>();
            session.getAttributeNames().asIterator().forEachRemaining(attributeNames::add);
            response.put("sessionAttributes", attributeNames);
        } else {
            response.put("session", "none");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Validate current session and return user information
     * GET /api/auth/validate
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateSession(@AuthenticationPrincipal User user,
                                           HttpServletRequest httpRequest) {
        log.debug("Session validation request received");
        
        try {
            HttpSession session = httpRequest.getSession(false);
            
            if (session == null || user == null) {
                Map<String, Object> response = createErrorResponse(
                    "Invalid Session", 
                    "No valid session found", 
                    HttpStatus.UNAUTHORIZED.value()
                );
                response.put("valid", false);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // Check if session attributes are present
            Boolean authenticated = (Boolean) session.getAttribute("authenticated");
            User sessionUser = (User) session.getAttribute("user");
            LocalDateTime loginTime = (LocalDateTime) session.getAttribute("loginTime");
            
            if (authenticated != null && authenticated && sessionUser != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("valid", true);
                response.put("user", user);
                response.put("sessionId", session.getId());
                response.put("sessionTimeout", session.getMaxInactiveInterval());
                response.put("loginTime", loginTime);
                response.put("lastAccessedTime", LocalDateTime.now());
                
                log.debug("Session validated successfully for user: {}", user.getEmail());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = createErrorResponse(
                    "Invalid Session", 
                    "Session is invalid or expired", 
                    HttpStatus.UNAUTHORIZED.value()
                );
                response.put("valid", false);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
        } catch (Exception e) {
            log.error("Error during session validation: {}", e.getMessage(), e);
            Map<String, Object> response = createErrorResponse(
                "Session Validation Failed", 
                "An error occurred while validating the session", 
                HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
            response.put("valid", false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Logout and invalidate session
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest httpRequest) {
        log.debug("Logout request received");
        
        try {
            HttpSession session = httpRequest.getSession(false);
            
            if (session != null) {
                String sessionId = session.getId();
                User user = (User) session.getAttribute("user");
                
                // Invalidate session
                session.invalidate();
                
                // Clear security context
                SecurityContextHolder.clearContext();
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Logged out successfully");
                response.put("sessionId", sessionId);
                response.put("logoutTime", LocalDateTime.now());
                
                if (user != null) {
                    log.info("User logged out successfully: {}", user.getEmail());
                } else {
                    log.info("Session invalidated successfully: {}", sessionId);
                }
                
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "No active session to logout");
                response.put("logoutTime", LocalDateTime.now());
                
                log.debug("No active session found for logout");
                return ResponseEntity.ok(response);
            }
            
        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = createErrorResponse(
                "Logout Failed", 
                "An error occurred during logout", 
                HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get CSRF token for the current session
     * GET /api/auth/csrf
     */
    @GetMapping("/csrf")
    public ResponseEntity<?> getCsrfToken(HttpServletRequest httpRequest) {
        log.debug("CSRF token request received");
        
        try {
            CsrfToken csrfToken = (CsrfToken) httpRequest.getAttribute(CsrfToken.class.getName());
            
            if (csrfToken != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("token", csrfToken.getToken());
                response.put("headerName", csrfToken.getHeaderName());
                response.put("parameterName", csrfToken.getParameterName());
                
                log.debug("CSRF token provided successfully");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = createErrorResponse(
                    "CSRF Token Unavailable", 
                    "CSRF token is not available", 
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
                );
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
            
        } catch (Exception e) {
            log.error("Error retrieving CSRF token: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = createErrorResponse(
                "CSRF Token Failed", 
                "An error occurred while retrieving CSRF token", 
                HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    // Helper methods for error handling
    
    private Map<String, Object> createErrorResponse(String error, String message, int status) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        errorResponse.put("status", status);
        errorResponse.put("timestamp", LocalDateTime.now());
        return errorResponse;
    }
    
    private Map<String, Object> createValidationErrorResponse(BindingResult bindingResult) {
        Map<String, Object> errorResponse = createErrorResponse(
            "Validation Failed", 
            "Invalid input data", 
            HttpStatus.BAD_REQUEST.value()
        );
        
        Map<String, String> fieldErrors = bindingResult.getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                fieldError -> fieldError.getField(),
                fieldError -> fieldError.getDefaultMessage(),
                (existing, replacement) -> existing // Keep first error message for duplicate fields
            ));
        
        errorResponse.put("fieldErrors", fieldErrors);
        return errorResponse;
    }
}