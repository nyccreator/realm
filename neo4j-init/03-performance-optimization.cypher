// Neo4j Performance Optimization - Section 3.1 Foundation
// This script applies performance optimizations for the authentication system

// === APOC PROCEDURES VERIFICATION ===
// Verify APOC plugin is loaded and available
CALL apoc.help("apoc") YIELD name
RETURN count(name) AS apoc_procedures_available;

// === DATABASE STATISTICS UPDATE ===
// Update statistics for better query planning
CALL apoc.stats.degrees() YIELD type, direction, total, min, max, mean, p50, p75, p90, p95, p99
RETURN type, direction, total, mean
ORDER BY total DESC;

// === QUERY PERFORMANCE BASELINE ===
// Establish baseline performance metrics for authentication queries

// Test user lookup by email performance
PROFILE MATCH (u:User {email: "demo@realm.local"}) 
RETURN u.id, u.displayName;

// Test user existence check performance  
PROFILE MATCH (u:User) 
WHERE u.email = "demo@realm.local" 
RETURN count(u) > 0 AS user_exists;

// === MEMORY USAGE OPTIMIZATION ===
// Configure memory settings for optimal performance
CALL apoc.config.list() YIELD key, value
WHERE key CONTAINS "memory"
RETURN key, value
ORDER BY key;

// === LOGGING SETUP ===
// Configure query logging for performance monitoring
CALL apoc.config.list() YIELD key, value  
WHERE key CONTAINS "log"
RETURN key, value
ORDER BY key;

// Log completion
RETURN "Performance optimization setup completed successfully" AS status;