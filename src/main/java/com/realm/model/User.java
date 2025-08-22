package com.realm.model;

import org.springframework.data.neo4j.core.schema.*;
import org.springframework.data.annotation.Id;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.*;

/**
 * User entity representing the single authenticated user in the PKM system.
 * This serves as the foundation for all graph relationships and knowledge management operations.
 * 
 * Graph Design: The User node is the central anchor point for all personal knowledge management
 * operations, with relationships to Notes, Tags, and other entities flowing from this node.
 */
@Node("User")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {
    
    @Id 
    @GeneratedValue
    private String id;
    
    @Property("email")
    @Email(message = "Please provide a valid email address")
    @NotBlank(message = "Email is required")
    private String email;
    
    @Property("passwordHash")
    @JsonIgnore
    private String passwordHash;
    
    @Property("displayName")
    @NotBlank(message = "Display name is required")
    @Size(min = 2, max = 50, message = "Display name must be between 2 and 50 characters")
    private String displayName;
    
    @Property("firstName")
    @Size(max = 50, message = "First name cannot exceed 50 characters")
    private String firstName;
    
    @Property("lastName")
    @Size(max = 50, message = "Last name cannot exceed 50 characters")
    private String lastName;
    
    @Property("bio")
    @Size(max = 500, message = "Bio cannot exceed 500 characters")
    private String bio;
    
    @Property("profilePictureUrl")
    private String profilePictureUrl;
    
    @Property("preferences")
    @Builder.Default
    private Map<String, Object> preferences = new HashMap<>();
    
    @Property("isActive")
    @Builder.Default
    private boolean isActive = true;
    
    @Property("isVerified")
    @Builder.Default
    private boolean isVerified = false;
    
    @Property("lastLoginAt")
    private LocalDateTime lastLoginAt;
    
    @Property("createdAt")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Property("updatedAt")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Graph relationships will be established as the system expands
    // For now, we focus on the User node structure for authentication
    
    // UserDetails implementation for Spring Security
    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList(); // Single user, no roles needed
    }
    
    @Override
    @JsonIgnore
    public String getPassword() {
        return passwordHash;
    }
    
    @Override
    @JsonIgnore
    public String getUsername() {
        return email; // Using email as username
    }
    
    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return isActive;
    }
    
    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return isActive;
    }
    
    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return isActive;
    }
    
    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return isActive && isVerified;
    }
    
    // Utility methods for user management
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updateProfile(String displayName, String firstName, String lastName, String bio) {
        if (displayName != null && !displayName.trim().isEmpty()) {
            this.displayName = displayName.trim();
        }
        if (firstName != null) {
            this.firstName = firstName.trim();
        }
        if (lastName != null) {
            this.lastName = lastName.trim();
        }
        if (bio != null) {
            this.bio = bio.trim();
        }
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updatePreference(String key, Object value) {
        if (preferences == null) {
            preferences = new HashMap<>();
        }
        preferences.put(key, value);
        this.updatedAt = LocalDateTime.now();
    }
    
    public Object getPreference(String key) {
        return preferences != null ? preferences.get(key) : null;
    }
    
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return (firstName + " " + lastName).trim();
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return displayName;
    }
}