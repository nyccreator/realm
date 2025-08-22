package com.realm.config;

import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * Neo4j Performance Configuration for the Realm PKM System.
 * 
 * This configuration class optimizes Neo4j for graph-based knowledge management operations,
 * focusing on connection pooling, transaction management, and performance monitoring
 * specifically tuned for the patterns expected in a personal knowledge management system.
 * 
 * Performance Targets:
 * - Note creation/update: < 100ms
 * - Graph traversal queries: < 200ms for depth 3
 * - Search operations: < 300ms for full-text search
 * - Relationship analysis: < 500ms for complex patterns
 */
@Configuration
@Slf4j
public class Neo4jPerformanceConfig {
    
    @Value("${spring.neo4j.uri}")
    private String uri;
    
    @Value("${spring.neo4j.authentication.username}")
    private String username;
    
    @Value("${spring.neo4j.authentication.password}")
    private String password;
    
    @Autowired
    private Environment environment;
    
    @Bean
    public Driver neo4jDriver() {
        log.info("Configuring Neo4j driver with performance optimizations for PKM workloads");
        
        Config config = Config.builder()
            // Connection pool settings optimized for PKM operations
            .withMaxConnectionPoolSize(50)
            .withConnectionAcquisitionTimeout(30, TimeUnit.SECONDS)
            .withConnectionTimeout(15, TimeUnit.SECONDS)
            .withMaxTransactionRetryTime(15, TimeUnit.SECONDS)
            
            // Memory and performance optimizations
            .withMaxConnectionLifetime(1, TimeUnit.HOURS)
            .withConnectionLivenessCheckTimeout(10, TimeUnit.SECONDS)
            
            // Logging configuration for performance monitoring
            .withLogging(org.neo4j.driver.Logging.slf4j())
            
            // Enable metrics for performance monitoring
            .withDriverMetrics()
            
            .build();
        
        Driver driver = GraphDatabase.driver(uri, 
            org.neo4j.driver.AuthTokens.basic(username, password), config);
        
        log.info("Neo4j driver configured successfully with URI: {}", uri);
        return driver;
    }
    
    /**
     * Neo4jClient bean for low-level database operations
     */
    @Bean
    public Neo4jClient neo4jClient(Driver driver) {
        return Neo4jClient.create(driver);
    }
    
    /**
     * Neo4jTemplate bean for programmatic database operations
     */
    @Bean
    public Neo4jTemplate neo4jTemplate(Neo4jClient neo4jClient) {
        return new Neo4jTemplate(neo4jClient);
    }
    
    /**
     * Initialize performance optimizations after application context is fully loaded
     * This prevents circular dependency issues during startup
     * Disabled for test profile to avoid test interference
     */
    @EventListener(ContextRefreshedEvent.class)
    public void initializePerformanceOptimizations() {
        // Skip index creation in test profile to prevent test interference
        if (environment.acceptsProfiles("test")) {
            log.info("Skipping Neo4j performance optimizations in test profile");
            return;
        }
        
        log.info("Initializing Neo4j performance optimizations for PKM system");
        createOptimalIndexes();
    }
    
    /**
     * Creates performance-optimized indexes for PKM operations.
     * These indexes are specifically designed for the query patterns
     * expected in a personal knowledge management system.
     */
    private void createOptimalIndexes() {
        try (var session = neo4jDriver().session()) {
            log.info("Creating performance indexes for PKM operations");
            
            // User indexes for authentication and user lookups
            session.run("CREATE INDEX user_email_index IF NOT EXISTS FOR (u:User) ON (u.email)");
            session.run("CREATE INDEX user_active_index IF NOT EXISTS FOR (u:User) ON (u.isActive)");
            
            // Note indexes for content operations
            session.run("CREATE INDEX note_title_index IF NOT EXISTS FOR (n:Note) ON (n.title)");
            session.run("CREATE INDEX note_status_index IF NOT EXISTS FOR (n:Note) ON (n.status)");
            session.run("CREATE INDEX note_created_index IF NOT EXISTS FOR (n:Note) ON (n.createdAt)");
            session.run("CREATE INDEX note_updated_index IF NOT EXISTS FOR (n:Note) ON (n.updatedAt)");
            session.run("CREATE INDEX note_favorite_index IF NOT EXISTS FOR (n:Note) ON (n.isFavorite)");
            session.run("CREATE INDEX note_category_index IF NOT EXISTS FOR (n:Note) ON (n.category)");
            session.run("CREATE INDEX note_tags_index IF NOT EXISTS FOR (n:Note) ON (n.tags)");
            
            // Full-text indexes for search operations
            session.run("CREATE FULLTEXT INDEX note_content_fulltext IF NOT EXISTS FOR (n:Note) ON EACH [n.title, n.content, n.summary]");
            
            // Composite indexes for complex queries
            session.run("CREATE INDEX note_user_status_index IF NOT EXISTS FOR (n:Note) ON (n.status, n.createdAt)");
            session.run("CREATE INDEX note_user_favorite_index IF NOT EXISTS FOR (n:Note) ON (n.isFavorite, n.updatedAt)");
            
            // Relationship indexes for graph traversal optimization
            session.run("CREATE INDEX notelink_type_index IF NOT EXISTS FOR ()-[r:REFERENCES]-() ON (r.type)");
            session.run("CREATE INDEX notelink_strength_index IF NOT EXISTS FOR ()-[r:REFERENCES]-() ON (r.strength)");
            session.run("CREATE INDEX notelink_created_index IF NOT EXISTS FOR ()-[r:REFERENCES]-() ON (r.createdAt)");
            
            // Constraints for data integrity
            session.run("CREATE CONSTRAINT user_email_unique IF NOT EXISTS FOR (u:User) REQUIRE u.email IS UNIQUE");
            session.run("CREATE CONSTRAINT user_id_exists IF NOT EXISTS FOR (u:User) REQUIRE u.id IS NOT NULL");
            session.run("CREATE CONSTRAINT note_id_exists IF NOT EXISTS FOR (n:Note) REQUIRE n.id IS NOT NULL");
            
            log.info("Performance indexes created successfully");
            
        } catch (Exception e) {
            log.error("Error creating performance indexes: {}", e.getMessage(), e);
        }
    }
    
    
    /**
     * Validates that all required indexes and constraints are in place
     * This method can be called during startup or health checks
     */
    public void validateSchemaIntegrity() {
        try (var session = neo4jDriver().session()) {
            log.info("Validating Neo4j schema integrity for PKM system");
            
            // Verify critical indexes exist
            var result = session.run("SHOW INDEXES YIELD name, type WHERE type = 'BTREE' RETURN count(*) as indexCount");
            if (result.hasNext()) {
                long indexCount = result.next().get("indexCount").asLong();
                log.info("Found {} BTREE indexes", indexCount);
                
                if (indexCount < 8) {
                    log.warn("Expected at least 8 performance indexes, found {}", indexCount);
                }
            }
            
            // Verify constraints exist
            var constraintResult = session.run("SHOW CONSTRAINTS YIELD name RETURN count(*) as constraintCount");
            if (constraintResult.hasNext()) {
                long constraintCount = constraintResult.next().get("constraintCount").asLong();
                log.info("Found {} constraints", constraintCount);
                
                if (constraintCount < 3) {
                    log.warn("Expected at least 3 constraints, found {}", constraintCount);
                }
            }
            
            log.info("Neo4j schema validation completed");
            
        } catch (Exception e) {
            log.error("Error validating schema integrity: {}", e.getMessage(), e);
        }
    }
    
}