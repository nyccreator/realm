package com.realm.controller;

import com.realm.model.User;
import com.realm.service.AuthService;
import com.realm.service.AuthService.AuthResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Authentication REST API Controller for the Realm PKM system.
 * 
 * This controller provides secure REST endpoints for all authentication operations
 * including user registration, login, token refresh, profile management, and
 * password operations. All endpoints return structured JSON responses with
 * proper HTTP status codes.
 * 
 * Endpoints:
 * - POST /api/auth/register - User registration
 * - POST /api/auth/login - User authentication
 * - POST /api/auth/refresh - Token refresh
 * - GET /api/auth/me - Current user profile
 * - PUT /api/auth/profile - Update user profile
 * - PUT /api/auth/password - Change password
 * - POST /api/auth/verify - Verify user account
 * - POST /api/auth/logout - User logout
 */
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {
    
    private final AuthService authService;
    
    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    /**
     * Register a new user
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.getEmail());
        
        AuthResult result = authService.registerUser(
            request.getEmail(),
            request.getPassword(),
            request.getDisplayName(),
            request.getFirstName(),
            request.getLastName()
        );
        
        if (result.isSuccess()) {
            Map<String, Object> data = createAuthResponseData(result);
            return ResponseEntity.ok(ApiResponse.success("User registered successfully", data));
        } else {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(result.getMessage()));
        }
    }
    
    /**
     * Authenticate user and return JWT tokens
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        
        AuthResult result = authService.authenticateUser(request.getEmail(), request.getPassword());
        
        if (result.isSuccess()) {
            Map<String, Object> data = createAuthResponseData(result);
            return ResponseEntity.ok(ApiResponse.success("Login successful", data));
        } else {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(result.getMessage()));
        }
    }
    
    /**
     * Refresh access token using refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.debug("Token refresh attempt");
        
        AuthResult result = authService.refreshToken(request.getRefreshToken());
        
        if (result.isSuccess()) {
            Map<String, Object> data = new HashMap<>();
            data.put("accessToken", result.getAccessToken());
            data.put("refreshToken", result.getRefreshToken());
            data.put("user", createUserResponse(result.getUser()));
            
            return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", data));
        } else {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(result.getMessage()));
        }
    }
    
    /**
     * Get current authenticated user profile
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("User not authenticated"));
        }
        
        User user = (User) authentication.getPrincipal();
        Map<String, Object> data = Map.of("user", createUserResponse(user));
        
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved", data));
    }
    
    /**
     * Update user profile
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request, 
                                                    Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("User not authenticated"));
        }
        
        User user = (User) authentication.getPrincipal();
        log.info("Profile update attempt for user: {}", user.getId());
        
        AuthResult result = authService.updateProfile(
            user.getId(),
            request.getDisplayName(),
            request.getFirstName(),
            request.getLastName(),
            request.getBio()
        );
        
        if (result.isSuccess()) {
            Map<String, Object> data = Map.of("user", createUserResponse(result.getUser()));
            return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", data));
        } else {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(result.getMessage()));
        }
    }
    
    /**
     * Change user password
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                     Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("User not authenticated"));
        }
        
        User user = (User) authentication.getPrincipal();
        log.info("Password change attempt for user: {}", user.getId());
        
        AuthResult result = authService.changePassword(
            user.getId(),
            request.getCurrentPassword(),
            request.getNewPassword()
        );
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
        } else {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(result.getMessage()));
        }
    }
    
    /**
     * Verify user account
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse> verifyAccount(@Valid @RequestBody VerifyAccountRequest request) {
        log.info("Account verification attempt for user: {}", request.getUserId());
        
        AuthResult result = authService.verifyUser(request.getUserId());
        
        if (result.isSuccess()) {
            Map<String, Object> data = Map.of("user", createUserResponse(result.getUser()));
            return ResponseEntity.ok(ApiResponse.success("Account verified successfully", data));
        } else {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(result.getMessage()));
        }
    }
    
    /**
     * Logout user (client-side token removal)
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            User user = (User) authentication.getPrincipal();
            log.info("User logout: {}", user.getEmail());
        }
        
        // Clear security context
        SecurityContextHolder.clearContext();
        
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }
    
    /**
     * Health check endpoint for authentication service
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse> health() {
        Map<String, Object> data = Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now(),
            "service", "Authentication Service"
        );
        
        return ResponseEntity.ok(ApiResponse.success("Authentication service is healthy", data));
    }
    
    // Helper methods
    
    private Map<String, Object> createAuthResponseData(AuthResult result) {
        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", result.getAccessToken());
        data.put("refreshToken", result.getRefreshToken());
        data.put("user", createUserResponse(result.getUser()));
        return data;
    }
    
    private Map<String, Object> createUserResponse(User user) {
        Map<String, Object> userResponse = new HashMap<>();
        userResponse.put("id", user.getId());
        userResponse.put("email", user.getEmail());
        userResponse.put("displayName", user.getDisplayName());
        userResponse.put("firstName", user.getFirstName());
        userResponse.put("lastName", user.getLastName());
        userResponse.put("fullName", user.getFullName());
        userResponse.put("bio", user.getBio());
        userResponse.put("profilePictureUrl", user.getProfilePictureUrl());
        userResponse.put("isActive", user.isActive());
        userResponse.put("isVerified", user.isVerified());
        userResponse.put("createdAt", user.getCreatedAt());
        userResponse.put("lastLoginAt", user.getLastLoginAt());
        return userResponse;
    }
    
    // Request/Response DTOs
    
    @Data
    public static class RegisterRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        private String email;
        
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        private String password;
        
        @NotBlank(message = "Display name is required")
        @Size(max = 100, message = "Display name cannot exceed 100 characters")
        private String displayName;
        
        @Size(max = 50, message = "First name cannot exceed 50 characters")
        private String firstName;
        
        @Size(max = 50, message = "Last name cannot exceed 50 characters")
        private String lastName;
    }
    
    @Data
    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        private String email;
        
        @NotBlank(message = "Password is required")
        private String password;
    }
    
    @Data
    public static class RefreshTokenRequest {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }
    
    @Data
    public static class UpdateProfileRequest {
        @Size(max = 100, message = "Display name cannot exceed 100 characters")
        private String displayName;
        
        @Size(max = 50, message = "First name cannot exceed 50 characters")
        private String firstName;
        
        @Size(max = 50, message = "Last name cannot exceed 50 characters")
        private String lastName;
        
        @Size(max = 500, message = "Bio cannot exceed 500 characters")
        private String bio;
    }
    
    @Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "Current password is required")
        private String currentPassword;
        
        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        private String newPassword;
    }
    
    @Data
    public static class VerifyAccountRequest {
        @NotBlank(message = "User ID is required")
        private String userId;
    }
    
    @Data
    public static class ApiResponse {
        private boolean success;
        private String message;
        private Object data;
        private LocalDateTime timestamp;
        
        private ApiResponse(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
            this.timestamp = LocalDateTime.now();
        }
        
        public static ApiResponse success(String message, Object data) {
            return new ApiResponse(true, message, data);
        }
        
        public static ApiResponse error(String message) {
            return new ApiResponse(false, message, null);
        }
    }
}