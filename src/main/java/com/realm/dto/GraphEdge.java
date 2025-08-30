package com.realm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing an edge (relationship) in the graph visualization.
 * Each edge represents a relationship between two notes in the PKM system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphEdge {
    
    /**
     * Unique identifier for this edge (corresponds to NoteLink ID)
     */
    private String id;
    
    /**
     * ID of the source node
     */
    private String source;
    
    /**
     * ID of the target node
     */
    private String target;
    
    /**
     * Type of relationship (REFERENCES, SUPPORTS, etc.)
     */
    @Builder.Default
    private String type = "REFERENCES";
    
    /**
     * Optional context describing the relationship
     */
    private String context;
    
    /**
     * Strength of the relationship (0.0 to 1.0)
     */
    @Builder.Default
    private double strength = 1.0;
    
    /**
     * Color of the edge (based on relationship type)
     */
    @Builder.Default
    private String color = "#999999";
    
    /**
     * Width/thickness of the edge line (based on strength)
     */
    @Builder.Default
    private double width = 2.0;
    
    /**
     * Whether this edge is bidirectional
     */
    @Builder.Default
    private boolean bidirectional = false;
    
    /**
     * Whether this edge is currently highlighted
     */
    @Builder.Default
    private boolean highlighted = false;
}