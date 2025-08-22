package com.realm;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic Spring Boot application context test.
 * This test verifies that the application context loads successfully
 * with all Neo4j configurations properly configured.
 */
@SpringBootTest(properties = {
    "spring.neo4j.uri=bolt://localhost:7687",
    "spring.neo4j.authentication.username=neo4j",
    "spring.neo4j.authentication.password=notverysecret",
    "spring.data.neo4j.database=neo4j"
})
@ActiveProfiles("test")
class RealmApplicationTests {

    @Test
    void contextLoads() {
        // This test verifies that the Spring application context
        // loads successfully with Neo4j configuration
    }

}
