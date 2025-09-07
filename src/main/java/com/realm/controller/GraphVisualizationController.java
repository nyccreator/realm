package com.realm.controller;

import com.realm.dto.GraphData;
import com.realm.dto.GraphNode;
import com.realm.model.User;
import com.realm.service.GraphVisualizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for graph visualization endpoints.
 * Provides APIs for interactive graph visualization of the knowledge network,
 * including full graph view, subgraph exploration, and node search functionality.
 */
@Slf4j
@RestController
@RequestMapping("/api/graph")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class GraphVisualizationController {
    
    @Autowired
    private GraphVisualizationService graphService;
    
    /**
     * Get complete graph data for visualization
     * 
     * @param user authenticated user
     * @param maxNodes optional limit on number of nodes (default 100)
     * @return GraphData containing nodes and edges for visualization
     */
    @GetMapping("/data")
    public ResponseEntity<GraphData> getGraphData(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Integer maxNodes) {
        
        log.debug("Getting graph data for user {} with maxNodes {}", user.getId(), maxNodes);
        
        try {
            GraphData graphData = graphService.getGraphData(user.getId(), maxNodes);
            
            log.info("Successfully generated graph data with {} nodes and {} edges for user {}", 
                    graphData.getTotalNodes(), graphData.getTotalEdges(), user.getId());
            
            return ResponseEntity.ok(graphData);
            
        } catch (Exception e) {
            log.error("Error generating graph data for user {}: {}", user.getId(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get subgraph focused on a specific node and its connections
     * 
     * @param nodeId the central node ID to focus on
     * @param depth optional depth of relationships to include (default 2, max 3)
     * @param user authenticated user
     * @return GraphData containing the focused subgraph
     */
    @GetMapping("/subgraph/{nodeId}")
    public ResponseEntity<GraphData> getSubgraph(
            @PathVariable String nodeId,
            @RequestParam(required = false) Integer depth,
            @AuthenticationPrincipal User user) {
        
        log.debug("Getting subgraph for user {} centered on node {} with depth {}", 
                 user.getId(), nodeId, depth);
        
        try {
            GraphData subgraph = graphService.getSubgraph(user.getId(), nodeId, depth);
            
            log.info("Successfully generated subgraph with {} nodes and {} edges for user {} centered on node {}", 
                    subgraph.getTotalNodes(), subgraph.getTotalEdges(), user.getId(), nodeId);
            
            return ResponseEntity.ok(subgraph);
            
        } catch (Exception e) {
            log.error("Error generating subgraph for user {} and node {}: {}", 
                     user.getId(), nodeId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Search for nodes matching a query string
     * 
     * @param query search query string
     * @param user authenticated user
     * @return List of matching GraphNodes with highlighting
     */
    @GetMapping("/search")
    public ResponseEntity<List<GraphNode>> searchNodes(
            @RequestParam String query,
            @AuthenticationPrincipal User user) {
        
        log.debug("Searching nodes for user {} with query '{}'", user.getId(), query);
        
        try {
            if (query == null || query.trim().isEmpty()) {
                log.debug("Empty search query for user {}", user.getId());
                return ResponseEntity.ok(List.of());
            }
            
            List<GraphNode> nodes = graphService.searchNodes(user.getId(), query);
            
            log.info("Found {} nodes matching query '{}' for user {}", 
                    nodes.size(), query, user.getId());
            
            return ResponseEntity.ok(nodes);
            
        } catch (Exception e) {
            log.error("Error searching nodes for user {} with query '{}': {}", 
                     user.getId(), query, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get graph statistics and metadata
     * 
     * @param user authenticated user
     * @return Graph statistics including total nodes, edges, and other metrics
     */
    @GetMapping("/stats")
    public ResponseEntity<GraphStats> getGraphStats(@AuthenticationPrincipal User user) {
        
        log.debug("Getting graph statistics for user {}", user.getId());
        
        try {
            GraphData fullGraph = graphService.getGraphData(user.getId(), null);
            
            GraphStats stats = GraphStats.builder()
                .totalNodes(fullGraph.getTotalNodes())
                .totalEdges(fullGraph.getTotalEdges())
                .build();
            
            // Calculate additional statistics
            if (fullGraph.getNodes() != null) {
                stats.setAvgConnectionsPerNode(
                    fullGraph.getTotalNodes() > 0 ? 
                    (double) fullGraph.getTotalEdges() / fullGraph.getTotalNodes() : 0.0
                );
                
                // Find most connected node
                GraphNode mostConnected = fullGraph.getNodes().stream()
                    .max((n1, n2) -> Integer.compare(n1.getConnectionCount(), n2.getConnectionCount()))
                    .orElse(null);
                
                if (mostConnected != null) {
                    stats.setMostConnectedNodeId(mostConnected.getId());
                    stats.setMaxConnections(mostConnected.getConnectionCount());
                }
            }
            
            log.info("Generated graph statistics for user {}: {} nodes, {} edges", 
                    user.getId(), stats.getTotalNodes(), stats.getTotalEdges());
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error generating graph statistics for user {}: {}", 
                     user.getId(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * DTO for graph statistics
     */
    public static class GraphStats {
        private int totalNodes;
        private int totalEdges;
        private double avgConnectionsPerNode;
        private String mostConnectedNodeId;
        private int maxConnections;
        
        public static GraphStatsBuilder builder() {
            return new GraphStatsBuilder();
        }
        
        // Getters and setters
        public int getTotalNodes() { return totalNodes; }
        public void setTotalNodes(int totalNodes) { this.totalNodes = totalNodes; }
        
        public int getTotalEdges() { return totalEdges; }
        public void setTotalEdges(int totalEdges) { this.totalEdges = totalEdges; }
        
        public double getAvgConnectionsPerNode() { return avgConnectionsPerNode; }
        public void setAvgConnectionsPerNode(double avgConnectionsPerNode) { 
            this.avgConnectionsPerNode = avgConnectionsPerNode; 
        }
        
        public String getMostConnectedNodeId() { return mostConnectedNodeId; }
        public void setMostConnectedNodeId(String mostConnectedNodeId) { 
            this.mostConnectedNodeId = mostConnectedNodeId; 
        }
        
        public int getMaxConnections() { return maxConnections; }
        public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }
        
        public static class GraphStatsBuilder {
            private GraphStats stats = new GraphStats();
            
            public GraphStatsBuilder totalNodes(int totalNodes) {
                stats.setTotalNodes(totalNodes);
                return this;
            }
            
            public GraphStatsBuilder totalEdges(int totalEdges) {
                stats.setTotalEdges(totalEdges);
                return this;
            }
            
            public GraphStats build() {
                return stats;
            }
        }
    }
}