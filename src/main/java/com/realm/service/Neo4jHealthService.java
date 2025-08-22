package com.realm.service;

import com.realm.config.Neo4jPerformanceConfig;
import com.realm.model.User;
import com.realm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Neo4j Health and Schema Validation Service for the Realm PKM System.
 * 
 * This service ensures that the Neo4j database is properly configured,
 * all required indexes and constraints are in place, and the schema
 * is ready for PKM operations. It performs validation checks during
 * application startup and provides health monitoring capabilities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Neo4jHealthService {
    
    private final Neo4jPerformanceConfig neo4jConfig;
    private final UserRepository userRepository;
    private final Environment environment;
    
    /**
     * Validates Neo4j schema integrity when the application starts
     * Skips validation in test profile to prevent test interference
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateSchemaOnStartup() {
        // Skip validation in test profile
        if (environment.acceptsProfiles("test")) {
            log.info("Skipping Neo4j schema validation in test profile");
            return;
        }
        
        log.info("Starting Neo4j schema validation for Realm PKM system");
        
        try {
            // Validate schema integrity
            neo4jConfig.validateSchemaIntegrity();
            
            // Test basic connectivity and operations (non-transactional)
            testBasicConnectivity();
            
            log.info("Neo4j schema validation completed successfully");
            
        } catch (Exception e) {
            log.error("Neo4j schema validation failed: {}", e.getMessage(), e);
            // Don't throw exception to prevent application startup failure
            log.warn("Continuing with application startup despite validation issues");
        }
    }
    
    /**
     * Tests basic Neo4j connectivity without transaction management
     * Used during application startup to avoid transaction conflicts
     */
    public void testBasicConnectivity() {
        try {
            log.info("Testing basic Neo4j connectivity");
            
            // Simple connectivity test without transactions
            long userCount = userRepository.count();
            log.info("Current user count: {}", userCount);
            
            log.info("Basic Neo4j connectivity test completed successfully");
            
        } catch (Exception e) {
            log.error("Basic connectivity test failed: {}", e.getMessage(), e);
            // Don't throw exception to prevent application startup failure
            log.warn("Neo4j connectivity test failed, but continuing startup");
        }
    }
    
    /**
     * Tests basic Neo4j operations to ensure schema is functional
     * This method can be called after startup for health checks
     */
    @Transactional(readOnly = true)
    public void testBasicOperations() {
        try {
            log.info("Testing basic Neo4j operations");
            
            // Test user repository functionality
            long userCount = userRepository.count();
            log.info("Current user count: {}", userCount);
            
            // Test email existence check (should not throw exception)
            boolean emailExists = userRepository.existsByEmail("test@example.com");
            log.debug("Email existence check successful: {}", emailExists);
            
            log.info("Basic Neo4j operations test completed successfully");
            
        } catch (Exception e) {
            log.error("Basic operations test failed: {}", e.getMessage(), e);
            throw new RuntimeException("Neo4j basic operations test failed", e);
        }
    }
    
    /**
     * Provides comprehensive health check information for monitoring
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Database connectivity
            health.put("connected", true);
            health.put("timestamp", LocalDateTime.now());
            
            // Basic counts
            long userCount = userRepository.count();
            health.put("userCount", userCount);
            
            // Performance metrics
            health.put("status", "healthy");
            
            log.debug("Neo4j health check completed successfully");
            
        } catch (Exception e) {
            log.error("Neo4j health check failed: {}", e.getMessage(), e);
            health.put("connected", false);
            health.put("status", "unhealthy");
            health.put("error", e.getMessage());
        }
        
        return health;
    }
    
    /**
     * Validates that the schema can handle basic PKM operations
     */
    @Transactional(readOnly = true)
    public boolean validatePkmReadiness() {
        try {
            // Test that repositories are working
            userRepository.count();
            
            // Test that we can perform basic queries without errors
            userRepository.existsByEmail("validation@test.com");
            
            log.info("PKM schema readiness validation passed");
            return true;
            
        } catch (Exception e) {
            log.error("PKM schema readiness validation failed: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Returns information about the current schema configuration
     */
    public Map<String, Object> getSchemaInfo() {
        Map<String, Object> schemaInfo = new HashMap<>();
        
        schemaInfo.put("databaseType", "Neo4j");
        schemaInfo.put("graphModel", "PKM Knowledge Graph");
        schemaInfo.put("entities", new String[]{"User", "Note", "NoteLink"});
        schemaInfo.put("primaryRelationships", new String[]{"CREATED_BY", "REFERENCES"});
        schemaInfo.put("indexOptimization", "Enabled");
        schemaInfo.put("performanceTargets", Map.of(
            "noteCreation", "< 100ms",
            "graphTraversal", "< 200ms (depth 3)",
            "search", "< 300ms",
            "relationshipAnalysis", "< 500ms"
        ));
        
        return schemaInfo;
    }
}