package com.realm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing the complete graph visualization data containing nodes and edges.
 * This DTO packages all the necessary information for rendering an interactive graph
 * visualization in the frontend React Flow component.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphData {
    
    /**
     * List of nodes (notes) in the graph visualization
     */
    private List<GraphNode> nodes;
    
    /**
     * List of edges (relationships) connecting the notes
     */
    private List<GraphEdge> edges;
    
    /**
     * ID of the center node for focused graph views (optional)
     */
    private String centerNodeId;
    
    /**
     * Total number of nodes in this graph data
     */
    private int totalNodes;
    
    /**
     * Total number of edges in this graph data
     */
    private int totalEdges;
    
    /**
     * Calculate totals based on the actual data
     */
    public void calculateTotals() {
        this.totalNodes = nodes != null ? nodes.size() : 0;
        this.totalEdges = edges != null ? edges.size() : 0;
    }
}