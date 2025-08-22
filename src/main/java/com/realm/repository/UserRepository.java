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
public interface UserRepository extends Neo4jRepository<User, Long> {
    
    /**
     * Find user by email (used for authentication)
     * This query leverages the email index for optimal performance
     */
    @Query("MATCH (u:User {email: $email}) RETURN u")
    Optional<User> findByEmail(@Param("email") String email);
    
    /**
     * Existence checks for registration validation
     */
    @Query("MATCH (u:User {email: $email}) RETURN count(u) > 0")
    boolean existsByEmail(@Param("email") String email);
    
    /**
     * Update last login timestamp
     */
    @Query("MATCH (u:User {id: $userId}) SET u.lastLoginAt = $timestamp RETURN u")
    Optional<User> updateLastLogin(@Param("userId") Long userId, @Param("timestamp") LocalDateTime timestamp);
    
    /**
     * For single-user MVP, get the primary user
     */
    @Query("MATCH (u:User) RETURN u ORDER BY u.createdAt ASC LIMIT 1")
    Optional<User> findPrimaryUser();
    
    /**
     * Find active user by email (used by AuthService)
     */
    @Query("MATCH (u:User {email: $email, isActive: true}) RETURN u")
    Optional<User> findActiveUserByEmail(@Param("email") String email);
    
    /**
     * Find verified user by email (used by tests)
     */
    @Query("MATCH (u:User {email: $email, isVerified: true}) RETURN u")
    Optional<User> findVerifiedUserByEmail(@Param("email") String email);
    
    /**
     * Update user verification status
     */
    @Query("MATCH (u:User {id: $userId}) SET u.isVerified = $verified, u.updatedAt = $timestamp RETURN u")
    Optional<User> updateVerificationStatus(@Param("userId") Long userId, 
                                           @Param("verified") boolean verified, 
                                           @Param("timestamp") LocalDateTime timestamp);
    
    /**
     * Update user active status
     */
    @Query("MATCH (u:User {id: $userId}) SET u.isActive = $active, u.updatedAt = $timestamp RETURN u")
    Optional<User> updateActiveStatus(@Param("userId") Long userId, 
                                     @Param("active") boolean active, 
                                     @Param("timestamp") LocalDateTime timestamp);
    
    /**
     * User statistics for dashboard
     */
    @Query("MATCH (u:User)-[:CREATED_BY]->(n:Note) WHERE u.id = $userId RETURN count(n)")
    Long countNotesByUser(@Param("userId") Long userId);
}