package com.realm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main application class for the Realm Personal Knowledge Management System.
 * 
 * This application leverages Neo4j as the primary database for graph-based knowledge management,
 * providing a foundation for interconnected notes, relationships, and knowledge discovery.
 * 
 * Key Features:
 * - Graph-first architecture with Neo4j
 * - Single-user authentication system
 * - Rich relationship modeling between notes
 * - Performance-optimized graph queries
 * - Spring Boot integration with Spring Data Neo4j
 */
@SpringBootApplication
@EnableNeo4jRepositories(basePackages = "com.realm.repository")
@EnableTransactionManagement
public class RealmApplication {

    public static void main(String[] args) {
        SpringApplication.run(RealmApplication.class, args);
    }

}
