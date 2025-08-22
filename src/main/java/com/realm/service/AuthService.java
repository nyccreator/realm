package com.realm.service;

import com.realm.model.User;
import com.realm.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Authentication Service for the Realm PKM system.
 * 
 * This service handles all authentication-related operations including user registration,
 * login, password management, and token operations. It implements UserDetailsService
 * for Spring Security integration and provides comprehensive authentication flows
 * optimized for the single-user PKM system.
 * 
 * Key Features:
 * - Secure user registration with validation
 * - JWT-based authentication
 * - Password strength validation
 * - Account verification flows
 * - Security logging and monitoring
 */
@Service
@Transactional
@Slf4j
public class AuthService implements UserDetailsService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    
    // Validation patterns
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    private static final Pattern STRONG_PASSWORD_PATTERN = 
        Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");
    
    @Autowired
    public AuthService(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      JwtTokenProvider jwtTokenProvider,
                      AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
    }
    
    /**
     * UserDetailsService implementation for Spring Security
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);
        
        User user = userRepository.findActiveUserByEmail(username)
                .orElseThrow(() -> {
                    log.warn("User not found or inactive: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });
        
        log.debug("User loaded successfully: {}", username);
        return user;
    }
    
    /**
     * Register a new user in the system
     */
    public AuthResult registerUser(String email, String password, String displayName, 
                                  String firstName, String lastName) {
        log.info("Attempting to register new user with email: {}", email);
        
        try {
            // Validate input parameters
            AuthResult validation = validateRegistrationInput(email, password, displayName);
            if (!validation.isSuccess()) {
                return validation;
            }
            
            // Check if user already exists
            if (userRepository.existsByEmail(email)) {
                log.warn("Registration failed - email already exists: {}", email);
                return AuthResult.failure("Email address is already registered");
            }
            
            // Create new user
            User user = User.builder()
                    .email(email.trim().toLowerCase())
                    .passwordHash(passwordEncoder.encode(password))
                    .displayName(displayName.trim())
                    .firstName(firstName != null ? firstName.trim() : null)
                    .lastName(lastName != null ? lastName.trim() : null)
                    .isActive(true)
                    .isVerified(false) // Will be verified later
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            // Save user to Neo4j
            User savedUser = userRepository.save(user);
            
            // Generate tokens
            String accessToken = jwtTokenProvider.generateTokenFromUsername(savedUser.getEmail());
            String refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.getEmail());
            
            log.info("User registered successfully: {}", email);
            return AuthResult.success("User registered successfully", accessToken, refreshToken, savedUser);
            
        } catch (Exception e) {
            log.error("Error during user registration for email {}: {}", email, e.getMessage(), e);
            return AuthResult.failure("Registration failed due to system error");
        }
    }
    
    /**
     * Authenticate user and generate tokens
     */
    public AuthResult authenticateUser(String email, String password) {
        log.info("Attempting to authenticate user: {}", email);
        
        try {
            // Validate input
            if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
                return AuthResult.failure("Email and password are required");
            }
            
            String normalizedEmail = email.trim().toLowerCase();
            
            // Authenticate with Spring Security
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, password)
            );
            
            // Load user details
            User user = userRepository.findActiveUserByEmail(normalizedEmail)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            
            // Update last login timestamp
            user.updateLastLogin();
            userRepository.save(user);
            
            // Generate tokens
            String accessToken = jwtTokenProvider.generateToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());
            
            log.info("User authenticated successfully: {}", email);
            return AuthResult.success("Authentication successful", accessToken, refreshToken, user);
            
        } catch (AuthenticationException e) {
            log.warn("Authentication failed for user {}: {}", email, e.getMessage());
            return AuthResult.failure("Invalid email or password");
        } catch (Exception e) {
            log.error("Error during authentication for user {}: {}", email, e.getMessage(), e);
            return AuthResult.failure("Authentication failed due to system error");
        }
    }
    
    /**
     * Refresh access token using refresh token
     */
    public AuthResult refreshToken(String refreshToken) {
        log.debug("Attempting to refresh token");
        
        try {
            // Validate refresh token
            if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
                log.warn("Invalid or expired refresh token");
                return AuthResult.failure("Invalid refresh token");
            }
            
            // Extract username and generate new access token
            String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
            if (username == null) {
                return AuthResult.failure("Invalid refresh token");
            }
            
            // Verify user still exists and is active
            User user = userRepository.findActiveUserByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            
            // Generate new access token
            String newAccessToken = jwtTokenProvider.generateTokenFromUsername(username);
            
            log.debug("Token refreshed successfully for user: {}", username);
            return AuthResult.success("Token refreshed successfully", newAccessToken, refreshToken, user);
            
        } catch (Exception e) {
            log.error("Error during token refresh: {}", e.getMessage(), e);
            return AuthResult.failure("Token refresh failed");
        }
    }
    
    /**
     * Verify user account
     */
    public AuthResult verifyUser(String userId) {
        log.info("Attempting to verify user: {}", userId);
        
        try {
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                return AuthResult.failure("User not found");
            }
            
            User user = userOptional.get();
            if (user.isVerified()) {
                return AuthResult.success("User is already verified", null, null, user);
            }
            
            // Update verification status
            userRepository.updateVerificationStatus(userId, true, LocalDateTime.now());
            user.setVerified(true);
            user.setUpdatedAt(LocalDateTime.now());
            
            log.info("User verified successfully: {}", userId);
            return AuthResult.success("User verified successfully", null, null, user);
            
        } catch (Exception e) {
            log.error("Error during user verification for user {}: {}", userId, e.getMessage(), e);
            return AuthResult.failure("Verification failed");
        }
    }
    
    /**
     * Change user password
     */
    public AuthResult changePassword(String userId, String currentPassword, String newPassword) {
        log.info("Attempting to change password for user: {}", userId);
        
        try {
            // Validate new password
            if (!isStrongPassword(newPassword)) {
                return AuthResult.failure("Password must be at least 8 characters with uppercase, lowercase, number, and special character");
            }
            
            // Load user
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            
            // Verify current password
            if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
                log.warn("Password change failed - incorrect current password for user: {}", userId);
                return AuthResult.failure("Current password is incorrect");
            }
            
            // Update password
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            
            log.info("Password changed successfully for user: {}", userId);
            return AuthResult.success("Password changed successfully", null, null, user);
            
        } catch (Exception e) {
            log.error("Error during password change for user {}: {}", userId, e.getMessage(), e);
            return AuthResult.failure("Password change failed");
        }
    }
    
    /**
     * Update user profile
     */
    public AuthResult updateProfile(String userId, String displayName, String firstName, 
                                   String lastName, String bio) {
        log.info("Attempting to update profile for user: {}", userId);
        
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            
            // Validate display name
            if (displayName != null && (displayName.trim().isEmpty() || displayName.length() > 100)) {
                return AuthResult.failure("Display name must be between 1 and 100 characters");
            }
            
            // Update profile
            user.updateProfile(displayName, firstName, lastName, bio);
            userRepository.save(user);
            
            log.info("Profile updated successfully for user: {}", userId);
            return AuthResult.success("Profile updated successfully", null, null, user);
            
        } catch (Exception e) {
            log.error("Error during profile update for user {}: {}", userId, e.getMessage(), e);
            return AuthResult.failure("Profile update failed");
        }
    }
    
    /**
     * Deactivate user account
     */
    public AuthResult deactivateUser(String userId) {
        log.info("Attempting to deactivate user: {}", userId);
        
        try {
            userRepository.updateActiveStatus(userId, false, LocalDateTime.now());
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            
            log.info("User deactivated successfully: {}", userId);
            return AuthResult.success("User deactivated successfully", null, null, user);
            
        } catch (Exception e) {
            log.error("Error during user deactivation for user {}: {}", userId, e.getMessage(), e);
            return AuthResult.failure("User deactivation failed");
        }
    }
    
    // Private helper methods
    
    private AuthResult validateRegistrationInput(String email, String password, String displayName) {
        if (email == null || email.trim().isEmpty()) {
            return AuthResult.failure("Email is required");
        }
        
        if (!isValidEmail(email)) {
            return AuthResult.failure("Please provide a valid email address");
        }
        
        if (password == null || password.isEmpty()) {
            return AuthResult.failure("Password is required");
        }
        
        if (!isStrongPassword(password)) {
            return AuthResult.failure("Password must be at least 8 characters with uppercase, lowercase, number, and special character");
        }
        
        if (displayName == null || displayName.trim().isEmpty()) {
            return AuthResult.failure("Display name is required");
        }
        
        if (displayName.length() > 100) {
            return AuthResult.failure("Display name cannot exceed 100 characters");
        }
        
        return AuthResult.success("Validation passed", null, null, null);
    }
    
    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }
    
    private boolean isStrongPassword(String password) {
        return password != null && STRONG_PASSWORD_PATTERN.matcher(password).matches();
    }
    
    /**
     * Result class for authentication operations
     */
    public static class AuthResult {
        private boolean success;
        private String message;
        private String accessToken;
        private String refreshToken;
        private User user;
        
        private AuthResult(boolean success, String message, String accessToken, String refreshToken, User user) {
            this.success = success;
            this.message = message;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.user = user;
        }
        
        public static AuthResult success(String message, String accessToken, String refreshToken, User user) {
            return new AuthResult(true, message, accessToken, refreshToken, user);
        }
        
        public static AuthResult failure(String message) {
            return new AuthResult(false, message, null, null, null);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public User getUser() { return user; }
    }
}