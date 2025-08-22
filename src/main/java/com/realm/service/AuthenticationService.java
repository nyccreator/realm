package com.realm.service;

import com.realm.model.User;
import com.realm.repository.UserRepository;
import com.realm.dto.LoginRequest;
import com.realm.dto.RegisterRequest;
import com.realm.dto.AuthResponse;
import com.realm.exception.AuthenticationException;
import com.realm.exception.UserAlreadyExistsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Authentication Service for Section 3.1 - Single-User Authentication
 * 
 * This service handles user registration, login, and token validation
 * exactly as specified in the development plan. It provides secure
 * JWT-based authentication for the single-user PKM system.
 */
@Service
@Transactional
public class AuthenticationService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    public AuthResponse register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + request.getEmail() + " already exists");
        }
        
        // Create new user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setDisplayName(request.getDisplayName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        
        // Save to Neo4j
        User savedUser = userRepository.save(user);
        
        // Generate JWT token
        String token = jwtTokenProvider.generateToken(savedUser);
        
        return new AuthResponse(token, savedUser);
    }
    
    public AuthResponse login(LoginRequest request) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            
            // Get user details
            User user = (User) authentication.getPrincipal();
            
            // Update last login timestamp
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
            
            // Generate JWT token
            String token = jwtTokenProvider.generateToken(user);
            
            return new AuthResponse(token, user);
            
        } catch (Exception e) {
            throw new AuthenticationException("Invalid email or password");
        }
    }
    
    public User getCurrentUser(String token) {
        String email = jwtTokenProvider.getEmailFromToken(token);
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new AuthenticationException("User not found"));
    }
}