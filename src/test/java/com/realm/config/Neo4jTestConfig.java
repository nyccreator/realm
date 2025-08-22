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
 * This configuration provides an isolated Neo4j test environment using Testcontainers
 * to ensure tests run with a clean database instance and don't interfere with each other.
 * 
 * The configuration relies on Spring Boot's auto-configuration for Neo4j, only providing
 * the connection properties via dynamic properties from the test container.
 */
@TestConfiguration
@Testcontainers
@Slf4j
public class Neo4jTestConfig {
    
    /**
     * Neo4j testcontainer for isolated test database
     */
    @Container
    static Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:5.15")
            .withAdminPassword("testpassword")
            .withReuse(true);
    
    /**
     * Configure Neo4j properties dynamically based on testcontainer
     * This overrides the application-test.properties settings
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        log.info("Configuring Neo4j test properties from testcontainer");
        registry.add("spring.neo4j.uri", neo4jContainer::getBoltUrl);
        registry.add("spring.neo4j.authentication.username", () -> "neo4j");
        registry.add("spring.neo4j.authentication.password", () -> "testpassword");
        registry.add("spring.data.neo4j.database", () -> "neo4j");
        
        // Test-specific connection pool settings
        registry.add("spring.neo4j.pool.max-connection-pool-size", () -> "5");
        registry.add("spring.neo4j.pool.connection-acquisition-timeout", () -> "10s");
        registry.add("spring.neo4j.pool.max-connection-lifetime", () -> "30m");
        registry.add("spring.neo4j.transaction.timeout", () -> "10s");
    }
}