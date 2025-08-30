package com.realm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO representing a node in the graph visualization.
 * Each node represents a note in the PKM system with visual properties
 * for rendering in the interactive graph component.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphNode {
    
    /**
     * Unique identifier for this node (corresponds to Note ID)
     */
    private String id;
    
    /**
     * Display title of the note
     */
    private String title;
    
    /**
     * Truncated content for preview purposes
     */
    private String content;
    
    /**
     * Tags associated with this note
     */
    private List<String> tags;
    
    /**
     * When the note was created
     */
    private LocalDateTime createdAt;
    
    /**
     * When the note was last updated
     */
    private LocalDateTime updatedAt;
    
    /**
     * Visual size of the node (based on content length and connections)
     */
    @Builder.Default
    private int size = 30;
    
    /**
     * Color of the node (based on tags or recency)
     */
    @Builder.Default
    private String color = "#9E9E9E";
    
    /**
     * X coordinate for saving node positions (optional)
     */
    private Double x;
    
    /**
     * Y coordinate for saving node positions (optional)
     */
    private Double y;
    
    /**
     * Number of connections this node has (for sizing calculation)
     */
    @Builder.Default
    private int connectionCount = 0;
    
    /**
     * Whether this node is currently selected
     */
    @Builder.Default
    private boolean selected = false;
    
    /**
     * Whether this node matches current search criteria
     */
    @Builder.Default
    private boolean highlighted = false;
}