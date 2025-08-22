package com.realm.config;

import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;
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
@EnableTransactionManagement
@Slf4j
public class Neo4jPerformanceConfig extends AbstractNeo4jConfig {
    
    @Value("${spring.neo4j.uri}")
    private String uri;
    
    @Value("${spring.neo4j.authentication.username}")
    private String username;
    
    @Value("${spring.neo4j.authentication.password}")
    private String password;
    
    @Override
    @Bean
    public Driver driver() {
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
    
    @PostConstruct
    public void initializePerformanceOptimizations() {
        log.info("Initializing Neo4j performance optimizations for PKM system");
        createOptimalIndexes();
    }
    
    /**
     * Creates performance-optimized indexes for PKM operations.
     * These indexes are specifically designed for the query patterns
     * expected in a personal knowledge management system.
     */
    private void createOptimalIndexes() {
        try (var session = driver().session()) {
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
     * Optimized transaction configuration for PKM operations
     */
    @Override
    protected java.util.Collection<String> getMappingBasePackages() {
        return java.util.List.of("com.realm.model");
    }
}