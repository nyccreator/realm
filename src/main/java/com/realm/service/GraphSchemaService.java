package com.realm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Graph Schema Service for advanced Neo4j schema operations in the Realm PKM System.
 * 
 * This service provides utilities for analyzing and optimizing the graph schema,
 * including relationship statistics, node connectivity analysis, and performance
 * optimization recommendations based on actual usage patterns.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GraphSchemaService {
    
    private final Driver driver;
    
    /**
     * Analyzes the current graph structure and provides insights
     */
    public Map<String, Object> analyzeGraphStructure() {
        Map<String, Object> analysis = new HashMap<>();
        
        try (var session = driver.session()) {
            log.info("Analyzing graph structure for optimization insights");
            
            // Node count analysis
            var nodeStats = session.run("""
                MATCH (n)
                RETURN labels(n) as nodeType, count(n) as count
                ORDER BY count DESC
                """);
            
            Map<String, Long> nodeCounts = new HashMap<>();
            while (nodeStats.hasNext()) {
                var record = nodeStats.next();
                var labels = record.get("nodeType").asList();
                var count = record.get("count").asLong();
                nodeCounts.put(labels.toString(), count);
            }
            analysis.put("nodeCounts", nodeCounts);
            
            // Relationship analysis
            var relationshipStats = session.run("""
                MATCH ()-[r]->()
                RETURN type(r) as relationshipType, count(r) as count
                ORDER BY count DESC
                """);
            
            Map<String, Long> relationshipCounts = new HashMap<>();
            while (relationshipStats.hasNext()) {
                var record = relationshipStats.next();
                var type = record.get("relationshipType").asString();
                var count = record.get("count").asLong();
                relationshipCounts.put(type, count);
            }
            analysis.put("relationshipCounts", relationshipCounts);
            
            // Graph connectivity metrics
            var connectivityStats = session.run("""
                MATCH (n:Note)
                OPTIONAL MATCH (n)-[out]->()\n                OPTIONAL MATCH ()-[in]->(n)
                RETURN 
                    avg(size(collect(DISTINCT out))) as avgOutDegree,
                    avg(size(collect(DISTINCT in))) as avgInDegree,
                    max(size(collect(DISTINCT out)) + size(collect(DISTINCT in))) as maxConnectivity
                """);
            
            if (connectivityStats.hasNext()) {
                var record = connectivityStats.next();
                Map<String, Object> connectivity = new HashMap<>();
                connectivity.put("averageOutDegree", record.get("avgOutDegree").asDouble());
                connectivity.put("averageInDegree", record.get("avgInDegree").asDouble());
                connectivity.put("maxConnectivity", record.get("maxConnectivity").asLong());
                analysis.put("connectivity", connectivity);
            }
            
            log.info("Graph structure analysis completed");
            
        } catch (Exception e) {
            log.error("Error analyzing graph structure: {}", e.getMessage(), e);
            analysis.put("error", e.getMessage());
        }
        
        return analysis;
    }
    
    /**
     * Provides recommendations for schema optimization based on current usage
     */
    public List<String> getOptimizationRecommendations() {
        List<String> recommendations = new ArrayList<>();
        
        try (var session = driver.session()) {
            log.info("Generating schema optimization recommendations");
            
            // Check for unused indexes
            var indexUsage = session.run("""
                SHOW INDEXES YIELD name, type, state
                WHERE state = 'ONLINE' AND type = 'BTREE'
                RETURN name, type
                """);
            
            int indexCount = 0;
            while (indexUsage.hasNext()) {
                indexUsage.next();
                indexCount++;
            }
            
            if (indexCount > 15) {
                recommendations.add("Consider reviewing index usage - high number of indexes detected");
            }
            
            // Check for orphaned nodes
            var orphanCheck = session.run("""
                MATCH (n:Note)
                WHERE NOT (n)-[:REFERENCES]-() AND NOT ()-[:REFERENCES]->(n)
                RETURN count(n) as orphanCount
                """);
            
            if (orphanCheck.hasNext()) {
                long orphanCount = orphanCheck.next().get("orphanCount").asLong();
                if (orphanCount > 10) {
                    recommendations.add("High number of orphaned notes detected - consider implementing auto-linking");
                }
            }
            
            // Check relationship density
            var densityCheck = session.run("""
                MATCH (n:Note)
                OPTIONAL MATCH (n)-[r:REFERENCES]->()
                WITH n, count(r) as outgoing
                WHERE outgoing = 0
                RETURN count(n) as isolatedNotes
                """);
            
            if (densityCheck.hasNext()) {
                long isolatedCount = densityCheck.next().get("isolatedNotes").asLong();
                if (isolatedCount > 0) {
                    recommendations.add(String.format("Found %d notes without outgoing links - consider relationship suggestions", isolatedCount));
                }
            }
            
            if (recommendations.isEmpty()) {
                recommendations.add("Graph schema is well-optimized for current usage patterns");
            }
            
            log.info("Generated {} optimization recommendations", recommendations.size());
            
        } catch (Exception e) {
            log.error("Error generating optimization recommendations: {}", e.getMessage(), e);
            recommendations.add("Error analyzing schema: " + e.getMessage());
        }
        
        return recommendations;
    }
    
    /**
     * Validates graph data integrity
     */
    public Map<String, Object> validateDataIntegrity() {
        Map<String, Object> validation = new HashMap<>();
        List<String> issues = new ArrayList<>();
        
        try (var session = driver.session()) {
            log.info("Validating graph data integrity");
            
            // Check for users without required properties
            var userValidation = session.run("""
                MATCH (u:User)
                WHERE u.email IS NULL OR u.email = '' OR u.createdAt IS NULL
                RETURN count(u) as invalidUsers
                """);
            
            if (userValidation.hasNext()) {
                long invalidUsers = userValidation.next().get("invalidUsers").asLong();
                if (invalidUsers > 0) {
                    issues.add(String.format("Found %d users with missing required properties", invalidUsers));
                }
            }
            
            // Check for notes without creators
            var noteValidation = session.run("""
                MATCH (n:Note)
                WHERE NOT (n)-[:CREATED_BY]->(:User)
                RETURN count(n) as orphanedNotes
                """);
            
            if (noteValidation.hasNext()) {
                long orphanedNotes = noteValidation.next().get("orphanedNotes").asLong();
                if (orphanedNotes > 0) {
                    issues.add(String.format("Found %d notes without creators", orphanedNotes));
                }
            }
            
            // Check for broken references
            var referenceValidation = session.run("""
                MATCH (n:Note)-[r:REFERENCES]->(target)
                WHERE NOT (target:Note)
                RETURN count(r) as brokenReferences
                """);
            
            if (referenceValidation.hasNext()) {
                long brokenRefs = referenceValidation.next().get("brokenReferences").asLong();
                if (brokenRefs > 0) {
                    issues.add(String.format("Found %d broken note references", brokenRefs));
                }
            }
            
            validation.put("issues", issues);
            validation.put("isValid", issues.isEmpty());
            validation.put("checkedAt", new Date());
            
            log.info("Data integrity validation completed with {} issues", issues.size());
            
        } catch (Exception e) {
            log.error("Error validating data integrity: {}", e.getMessage(), e);
            issues.add("Validation error: " + e.getMessage());
            validation.put("issues", issues);
            validation.put("isValid", false);
        }
        
        return validation;
    }
    
    /**
     * Returns performance metrics for the current graph
     */
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try (var session = driver.session()) {
            log.info("Collecting graph performance metrics");
            
            // Query execution time sampling
            long startTime = System.currentTimeMillis();
            session.run("MATCH (u:User) RETURN count(u) as userCount");
            long userQueryTime = System.currentTimeMillis() - startTime;
            
            startTime = System.currentTimeMillis();
            session.run("MATCH (n:Note) RETURN count(n) as noteCount");
            long noteQueryTime = System.currentTimeMillis() - startTime;
            
            startTime = System.currentTimeMillis();
            session.run("MATCH ()-[r:REFERENCES]->() RETURN count(r) as refCount");
            long relationshipQueryTime = System.currentTimeMillis() - startTime;
            
            Map<String, Long> queryTimes = new HashMap<>();
            queryTimes.put("userQuery", userQueryTime);
            queryTimes.put("noteQuery", noteQueryTime);
            queryTimes.put("relationshipQuery", relationshipQueryTime);
            
            metrics.put("queryTimes", queryTimes);
            metrics.put("measuredAt", new Date());
            
            // Performance status based on target times
            String status = "healthy";
            if (userQueryTime > 100 || noteQueryTime > 100 || relationshipQueryTime > 200) {
                status = "degraded";
            }
            metrics.put("performanceStatus", status);
            
            log.info("Performance metrics collection completed");
            
        } catch (Exception e) {
            log.error("Error collecting performance metrics: {}", e.getMessage(), e);
            metrics.put("error", e.getMessage());
            metrics.put("performanceStatus", "error");
        }
        
        return metrics;
    }
}