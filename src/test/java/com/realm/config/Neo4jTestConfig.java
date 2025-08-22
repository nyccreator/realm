package com.realm.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import lombok.extern.slf4j.Slf4j;

/**
 * Neo4j Test Configuration for integration tests.
 * 
 * This configuration provides an isolated Neo4j test environment using TestContainers
 * to ensure tests run with a clean database instance and don't interfere with each other.
 * 
 * The configuration relies on Spring Boot's auto-configuration for Neo4j, only providing
 * the connection properties via dynamic properties from the test container.
 * 
 * Key features:
 * - Isolated Neo4j instance per test suite
 * - Automatic container lifecycle management
 * - Consistent test credentials
 * - Container reuse for performance
 */
@TestConfiguration
@Testcontainers
@Slf4j
public class Neo4jTestConfig {
    
    /**
     * Neo4j TestContainer for isolated test database.
     * Using Neo4j 5.15 with a consistent admin password for all tests.
     * Container reuse is enabled for better performance.
     */
    @Container
    static Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:5.15")
            .withAdminPassword("testpassword")
            .withReuse(false); // Disable reuse to ensure clean state for each test run
    
    /**
     * Configure Neo4j properties dynamically based on TestContainer.
     * This completely overrides any hardcoded connection settings from application-test.properties.
     * 
     * @param registry Dynamic property registry for Spring Boot test context
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        log.info("Starting Neo4j TestContainer for test configuration...");
        neo4jContainer.start();
        
        String boltUrl = neo4jContainer.getBoltUrl();
        log.info("Configuring Neo4j test properties from TestContainer: {}", boltUrl);
        
        // Core connection properties
        registry.add("spring.neo4j.uri", () -> boltUrl);
        registry.add("spring.neo4j.authentication.username", () -> "neo4j");
        registry.add("spring.neo4j.authentication.password", () -> "testpassword");
        registry.add("spring.data.neo4j.database", () -> "neo4j");
        
        // Test-optimized connection pool settings
        registry.add("spring.neo4j.pool.max-connection-pool-size", () -> "5");
        registry.add("spring.neo4j.pool.connection-acquisition-timeout", () -> "30s");
        registry.add("spring.neo4j.pool.max-connection-lifetime", () -> "30m");
        registry.add("spring.neo4j.pool.idle-time-before-connection-test", () -> "10s");
        registry.add("spring.neo4j.transaction.timeout", () -> "30s");
        
        log.info("Neo4j TestContainer configuration completed successfully");
    }
    
    /**
     * Get the running Neo4j container instance.
     * Useful for custom operations during tests.
     * 
     * @return The Neo4j container instance
     */
    public static Neo4jContainer<?> getNeo4jContainer() {
        return neo4jContainer;
    }
}