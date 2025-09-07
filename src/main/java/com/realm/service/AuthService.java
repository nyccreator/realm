package com.realm.service;

import com.realm.model.User;
import com.realm.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Authentication Service for the Realm PKM system.
 * 
 * This service handles all authentication-related operations including user registration,
 * password management, and user profile operations. It implements UserDetailsService
 * for Spring Security integration and provides comprehensive authentication flows
 * optimized for session-based authentication in the single-user PKM system.
 * 
 * Key Features:
 * - Secure user registration with validation
 * - Session-based authentication via Spring Security
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
    
    // Validation patterns
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    private static final Pattern STRONG_PASSWORD_PATTERN = 
        Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");
    
    @Autowired
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
     * Create a new user in the system (simplified for session-based auth)
     */
    public User createUser(String email, String password, String displayName) {
        log.info("Attempting to create new user with email: {}", email);
        
        // Validate input parameters
        validateRegistrationInput(email, password, displayName);
        
        // Check if user already exists
        if (userRepository.existsByEmail(email)) {
            log.warn("Registration failed - email already exists: {}", email);
            throw new com.realm.exception.UserAlreadyExistsException("Email address is already registered");
        }
        
        // Create new user
        User user = User.builder()
                .email(email.trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(password))
                .displayName(displayName.trim())
                .isActive(true)
                .isVerified(true) // Auto-verify for simplicity in session-based system
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        // Save user to Neo4j
        User savedUser = userRepository.save(user);
        log.info("User created successfully: {}", email);
        return savedUser;
    }
    
    /**
     * Update user last login timestamp (called after successful authentication)
     */
    public void updateLastLogin(String email) {
        log.debug("Updating last login timestamp for user: {}", email);
        
        try {
            User user = userRepository.findActiveUserByEmail(email.trim().toLowerCase())
                    .orElse(null);
            
            if (user != null) {
                user.updateLastLogin();
                userRepository.save(user);
                log.debug("Last login timestamp updated for user: {}", email);
            }
        } catch (Exception e) {
            log.warn("Failed to update last login timestamp for user {}: {}", email, e.getMessage());
        }
    }
    
    /**
     * Verify user account
     */
    public User verifyUser(String userId) {
        log.info("Attempting to verify user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        if (!user.isVerified()) {
            userRepository.updateVerificationStatus(userId, true, LocalDateTime.now());
            user.setVerified(true);
            user.setUpdatedAt(LocalDateTime.now());
            log.info("User verified successfully: {}", userId);
        }
        
        return user;
    }
    
    /**
     * Change user password
     */
    public User changePassword(String userId, String currentPassword, String newPassword) {
        log.info("Attempting to change password for user: {}", userId);
        
        // Validate new password
        if (!isStrongPassword(newPassword)) {
            throw new com.realm.exception.ValidationException("Password must be at least 8 characters with uppercase, lowercase, number, and special character");
        }
        
        // Load user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            log.warn("Password change failed - incorrect current password for user: {}", userId);
            throw new com.realm.exception.AuthenticationException("Current password is incorrect");
        }
        
        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        
        log.info("Password changed successfully for user: {}", userId);
        return savedUser;
    }
    
    /**
     * Update user profile
     */
    public User updateProfile(String userId, String displayName, String firstName, 
                             String lastName, String bio) {
        log.info("Attempting to update profile for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        // Validate display name
        if (displayName != null && (displayName.trim().isEmpty() || displayName.length() > 100)) {
            throw new com.realm.exception.ValidationException("Display name must be between 1 and 100 characters");
        }
        
        // Update profile
        user.updateProfile(displayName, firstName, lastName, bio);
        User savedUser = userRepository.save(user);
        
        log.info("Profile updated successfully for user: {}", userId);
        return savedUser;
    }
    
    /**
     * Deactivate user account
     */
    public User deactivateUser(String userId) {
        log.info("Attempting to deactivate user: {}", userId);
        
        userRepository.updateActiveStatus(userId, false, LocalDateTime.now());
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        log.info("User deactivated successfully: {}", userId);
        return user;
    }
    
    // Private helper methods
    
    private void validateRegistrationInput(String email, String password, String displayName) {
        if (email == null || email.trim().isEmpty()) {
            throw new com.realm.exception.ValidationException("Email is required");
        }
        
        if (!isValidEmail(email)) {
            throw new com.realm.exception.ValidationException("Please provide a valid email address");
        }
        
        if (password == null || password.isEmpty()) {
            throw new com.realm.exception.ValidationException("Password is required");
        }
        
        if (!isStrongPassword(password)) {
            throw new com.realm.exception.ValidationException("Password must be at least 8 characters with uppercase, lowercase, number, and special character");
        }
        
        if (displayName == null || displayName.trim().isEmpty()) {
            throw new com.realm.exception.ValidationException("Display name is required");
        }
        
        if (displayName.length() > 100) {
            throw new com.realm.exception.ValidationException("Display name cannot exceed 100 characters");
        }
    }
    
    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }
    
    private boolean isStrongPassword(String password) {
        return password != null && STRONG_PASSWORD_PATTERN.matcher(password).matches();
    }
}