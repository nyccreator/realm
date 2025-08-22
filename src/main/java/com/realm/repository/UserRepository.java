package com.realm.repository;

import com.realm.model.User;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Neo4j Repository for User entity operations.
 * 
 * This repository provides graph-optimized queries for user management
 * in the single-user authentication system. All queries are designed
 * to leverage Neo4j's graph performance characteristics.
 */
@Repository
public interface UserRepository extends Neo4jRepository<User, String> {
    
    /**
     * Find user by email (used for authentication)
     * This query leverages the email index for optimal performance
     */
    @Query("MATCH (u:User {email: $email}) RETURN u")
    Optional<User> findByEmail(@Param("email") String email);
    
    /**
     * Find active user by email
     * Optimized for authentication flows
     */
    @Query("MATCH (u:User {email: $email, isActive: true}) RETURN u")
    Optional<User> findActiveUserByEmail(@Param("email") String email);
    
    /**
     * Find verified user by email
     * Used for secure operations that require verified users
     */
    @Query("MATCH (u:User {email: $email, isActive: true, isVerified: true}) RETURN u")
    Optional<User> findVerifiedUserByEmail(@Param("email") String email);
    
    /**
     * Update user's last login timestamp
     * Optimized single-property update
     */
    @Query("MATCH (u:User {id: $userId}) SET u.lastLoginAt = $timestamp, u.updatedAt = $timestamp RETURN u")
    Optional<User> updateLastLogin(@Param("userId") String userId, @Param("timestamp") LocalDateTime timestamp);
    
    /**
     * Check if email exists (for registration validation)
     * Returns boolean result for efficient existence checks
     */
    @Query("MATCH (u:User {email: $email}) RETURN count(u) > 0")
    boolean existsByEmail(@Param("email") String email);
    
    /**
     * Update user verification status
     * Used for account verification flows
     */
    @Query("MATCH (u:User {id: $userId}) SET u.isVerified = $verified, u.updatedAt = $timestamp RETURN u")
    Optional<User> updateVerificationStatus(@Param("userId") String userId, 
                                          @Param("verified") boolean verified, 
                                          @Param("timestamp") LocalDateTime timestamp);
    
    /**
     * Update user active status
     * Used for account activation/deactivation
     */
    @Query("MATCH (u:User {id: $userId}) SET u.isActive = $active, u.updatedAt = $timestamp RETURN u")
    Optional<User> updateActiveStatus(@Param("userId") String userId, 
                                    @Param("active") boolean active, 
                                    @Param("timestamp") LocalDateTime timestamp);
    
    /**
     * Get user profile with basic stats
     * Returns user with computed statistics for dashboard
     */
    @Query("""
        MATCH (u:User {id: $userId})
        OPTIONAL MATCH (u)<-[:CREATED_BY]-(n:Note)
        RETURN u, 
               count(n) as noteCount,
               max(n.updatedAt) as lastNoteUpdate
        """)
    Optional<User> findUserWithStats(@Param("userId") String userId);
    
    /**
     * Update user preferences
     * Optimized for preference updates without full entity loading
     */
    @Query("MATCH (u:User {id: $userId}) SET u.preferences = $preferences, u.updatedAt = $timestamp RETURN u")
    Optional<User> updatePreferences(@Param("userId") String userId, 
                                   @Param("preferences") java.util.Map<String, Object> preferences,
                                   @Param("timestamp") LocalDateTime timestamp);
}