package com.realm.controller;

import com.realm.dto.AuthResponse;
import com.realm.dto.LoginRequest;
import com.realm.dto.RegisterRequest;
import com.realm.exception.AuthenticationException;
import com.realm.exception.UserAlreadyExistsException;
import com.realm.exception.ValidationException;
import com.realm.model.User;
import com.realm.service.AuthService;
import com.realm.service.AuthenticationService;
import com.realm.service.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Authentication REST API Controller for Section 3.1 - Single-User Authentication
 * 
 * This controller provides secure REST endpoints for authentication operations
 * as specified in the development plan:
 * - POST /api/auth/register - User registration
 * - POST /api/auth/login - User authentication
 * - GET /api/auth/validate - Token validation
 * - POST /api/auth/refresh - Token refresh
 * 
 * Features:
 * - Comprehensive input validation
 * - Structured error responses
 * - Security-focused logging
 * - JWT token management
 * - CORS-enabled endpoints
 */
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {
    
    private final AuthService authService;
    private final AuthenticationService authenticationService;
    private final JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    public AuthController(AuthService authService,
                         AuthenticationService authenticationService,
                         JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.authenticationService = authenticationService;
        this.jwtTokenProvider = jwtTokenProvider;
    }
    
    /**
     * Register a new user
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, BindingResult bindingResult) {
        log.info("Registration attempt for email: {}", request.getEmail());
        
        try {
            // Check for validation errors
            if (bindingResult.hasErrors()) {
                Map<String, Object> errorResponse = createValidationErrorResponse(bindingResult);
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Use the enhanced AuthService for registration
            AuthService.AuthResult result = authService.registerUser(
                request.getEmail(),
                request.getPassword(),
                request.getDisplayName(),
                null, // firstName - not in basic registration
                null  // lastName - not in basic registration
            );
            
            if (result.isSuccess()) {
                // Create response with both access and refresh tokens
                Map<String, Object> response = new HashMap<>();
                response.put("message", result.getMessage());
                response.put("token", result.getAccessToken()); // For backward compatibility
                response.put("accessToken", result.getAccessToken());
                response.put("refreshToken", result.getRefreshToken());
                response.put("user", result.getUser());
                
                log.info("User registered successfully: {}", request.getEmail());
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                Map<String, Object> errorResponse = createErrorResponse(
                    "Registration Failed", 
                    result.getMessage(), 
                    HttpStatus.BAD_REQUEST.value()
                );
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
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
     * Authenticate user and return JWT tokens
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, BindingResult bindingResult) {
        log.info("Login attempt for email: {}", request.getEmail());
        
        try {
            // Check for validation errors
            if (bindingResult.hasErrors()) {
                Map<String, Object> errorResponse = createValidationErrorResponse(bindingResult);
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Use the enhanced AuthService for authentication
            AuthService.AuthResult result = authService.authenticateUser(
                request.getEmail(),
                request.getPassword()
            );
            
            if (result.isSuccess()) {
                // Create response with both access and refresh tokens
                Map<String, Object> response = new HashMap<>();
                response.put("message", result.getMessage());
                response.put("token", result.getAccessToken()); // For backward compatibility
                response.put("accessToken", result.getAccessToken());
                response.put("refreshToken", result.getRefreshToken());
                response.put("user", result.getUser());
                
                log.info("User authenticated successfully: {}", request.getEmail());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = createErrorResponse(
                    "Authentication Failed", 
                    result.getMessage(), 
                    HttpStatus.UNAUTHORIZED.value()
                );
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
            
        } catch (AuthenticationException e) {
            log.warn("Authentication failed for {}: {}", request.getEmail(), e.getMessage());
            Map<String, Object> errorResponse = createErrorResponse(
                "Authentication Failed", 
                "Invalid email or password", 
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
     * Validate JWT token and return user information
     * GET /api/auth/validate
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                          @AuthenticationPrincipal User user) {
        log.debug("Token validation request received");
        
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                Map<String, Object> response = createErrorResponse(
                    "Invalid Token", 
                    "Authorization header missing or invalid format", 
                    HttpStatus.UNAUTHORIZED.value()
                );
                response.put("valid", false);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            String token = authHeader.substring(7);
            if (jwtTokenProvider.validateToken(token) && user != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("valid", true);
                response.put("user", user);
                response.put("tokenExpiry", jwtTokenProvider.getExpirationDateFromToken(token));
                response.put("remainingTime", jwtTokenProvider.getRemainingTimeToExpire(token));
                
                log.debug("Token validated successfully for user: {}", user.getEmail());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = createErrorResponse(
                    "Invalid Token", 
                    "Token is invalid or expired", 
                    HttpStatus.UNAUTHORIZED.value()
                );
                response.put("valid", false);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
        } catch (Exception e) {
            log.error("Error during token validation: {}", e.getMessage(), e);
            Map<String, Object> response = createErrorResponse(
                "Token Validation Failed", 
                "An error occurred while validating the token", 
                HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
            response.put("valid", false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Refresh access token using refresh token
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        log.debug("Token refresh request received");
        
        try {
            String refreshToken = request.get("refreshToken");
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                Map<String, Object> errorResponse = createErrorResponse(
                    "Invalid Request", 
                    "Refresh token is required", 
                    HttpStatus.BAD_REQUEST.value()
                );
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            AuthService.AuthResult result = authService.refreshToken(refreshToken);
            
            if (result.isSuccess()) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", result.getMessage());
                response.put("accessToken", result.getAccessToken());
                response.put("refreshToken", result.getRefreshToken());
                response.put("user", result.getUser());
                
                log.debug("Token refreshed successfully");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = createErrorResponse(
                    "Token Refresh Failed", 
                    result.getMessage(), 
                    HttpStatus.UNAUTHORIZED.value()
                );
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
            
        } catch (Exception e) {
            log.error("Error during token refresh: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = createErrorResponse(
                "Token Refresh Failed", 
                "An error occurred while refreshing the token", 
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