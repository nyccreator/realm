package com.realm.model;

import org.springframework.data.neo4j.core.schema.*;
import org.springframework.data.annotation.Id;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * NoteLink represents the relationship properties between Notes in the knowledge graph.
 * This class captures the semantic context and metadata of connections between notes,
 * enabling rich relationship modeling in the PKM system.
 * 
 * Graph Design: Instead of simple edges, NoteLink provides rich relationship context
 * that can include relationship types (references, contradicts, supports, etc.),
 * contextual information, strength metrics, and temporal data. This allows for
 * sophisticated graph analysis and knowledge discovery algorithms.
 */
@RelationshipProperties
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoteLink {
    
    @Id 
    @GeneratedValue
    private String id;
    
    @Property("type")
    @Builder.Default
    private String type = "REFERENCES"; // REFERENCES, SUPPORTS, CONTRADICTS, BUILDS_ON, RELATED_TO, etc.
    
    @Property("context")
    private String context; // User-provided context about the relationship
    
    @Property("strength")
    @Builder.Default
    private Double strength = 1.0; // Relationship strength (0.0 to 1.0)
    
    @Property("isInferred")
    @Builder.Default
    private boolean isInferred = false; // Whether the link was auto-discovered or user-created
    
    @Property("direction")
    @Builder.Default
    private String direction = "OUTGOING"; // OUTGOING, INCOMING, BIDIRECTIONAL
    
    @Property("metadata")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    @Property("createdAt")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Property("updatedAt")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Property("lastTraversedAt")
    private LocalDateTime lastTraversedAt; // When this link was last followed
    
    @Property("traversalCount")
    @Builder.Default
    private int traversalCount = 0; // How many times this link has been followed
    
    // The target note this relationship points to
    @TargetNode
    private Note targetNote;
    
    // Relationship type constants for better type safety
    public static final String TYPE_REFERENCES = "REFERENCES";
    public static final String TYPE_SUPPORTS = "SUPPORTS";
    public static final String TYPE_CONTRADICTS = "CONTRADICTS";
    public static final String TYPE_BUILDS_ON = "BUILDS_ON";
    public static final String TYPE_RELATED_TO = "RELATED_TO";
    public static final String TYPE_INSPIRED_BY = "INSPIRED_BY";
    public static final String TYPE_CLARIFIES = "CLARIFIES";
    public static final String TYPE_QUESTION = "QUESTION";
    public static final String TYPE_ANSWER = "ANSWER";
    public static final String TYPE_EXAMPLE = "EXAMPLE";
    
    // Direction constants
    public static final String DIRECTION_OUTGOING = "OUTGOING";
    public static final String DIRECTION_INCOMING = "INCOMING";
    public static final String DIRECTION_BIDIRECTIONAL = "BIDIRECTIONAL";
    
    // Constructors for common use cases
    public NoteLink(Note targetNote, String type, String context) {
        this();
        this.targetNote = targetNote;
        this.type = type;
        this.context = context;
    }
    
    public NoteLink(Note targetNote, String type) {
        this(targetNote, type, null);
    }
    
    public NoteLink(Note targetNote) {
        this(targetNote, TYPE_REFERENCES, null);
    }
    
    // Utility methods for link management
    public void updateContext(String newContext) {
        this.context = newContext;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updateType(String newType) {
        if (isValidType(newType)) {
            this.type = newType;
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    public void updateStrength(Double newStrength) {
        if (newStrength != null && newStrength >= 0.0 && newStrength <= 1.0) {
            this.strength = newStrength;
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    public void recordTraversal() {
        this.lastTraversedAt = LocalDateTime.now();
        this.traversalCount++;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
        this.updatedAt = LocalDateTime.now();
    }
    
    public Object getMetadata(String key) {
        return metadata != null ? metadata.get(key) : null;
    }
    
    public void removeMetadata(String key) {
        if (metadata != null) {
            metadata.remove(key);
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    // Validation methods
    public static boolean isValidType(String type) {
        return type != null && (
            type.equals(TYPE_REFERENCES) ||
            type.equals(TYPE_SUPPORTS) ||
            type.equals(TYPE_CONTRADICTS) ||
            type.equals(TYPE_BUILDS_ON) ||
            type.equals(TYPE_RELATED_TO) ||
            type.equals(TYPE_INSPIRED_BY) ||
            type.equals(TYPE_CLARIFIES) ||
            type.equals(TYPE_QUESTION) ||
            type.equals(TYPE_ANSWER) ||
            type.equals(TYPE_EXAMPLE)
        );
    }
    
    public static boolean isValidDirection(String direction) {
        return direction != null && (
            direction.equals(DIRECTION_OUTGOING) ||
            direction.equals(DIRECTION_INCOMING) ||
            direction.equals(DIRECTION_BIDIRECTIONAL)
        );
    }
    
    // Helper methods for relationship semantics
    public boolean isStrongLink() {
        return strength != null && strength >= 0.7;
    }
    
    public boolean isWeakLink() {
        return strength != null && strength < 0.3;
    }
    
    public boolean isFrequentlyTraversed() {
        return traversalCount >= 10;
    }
    
    public boolean isRecentlyCreated() {
        return createdAt != null && createdAt.isAfter(LocalDateTime.now().minusDays(7));
    }
    
    public boolean isRecentlyTraversed() {
        return lastTraversedAt != null && lastTraversedAt.isAfter(LocalDateTime.now().minusDays(1));
    }
    
    // Display methods
    public String getDisplayType() {
        if (type == null) return "References";
        
        switch (type) {
            case TYPE_REFERENCES: return "References";
            case TYPE_SUPPORTS: return "Supports";
            case TYPE_CONTRADICTS: return "Contradicts";
            case TYPE_BUILDS_ON: return "Builds On";
            case TYPE_RELATED_TO: return "Related To";
            case TYPE_INSPIRED_BY: return "Inspired By";
            case TYPE_CLARIFIES: return "Clarifies";
            case TYPE_QUESTION: return "Questions";
            case TYPE_ANSWER: return "Answers";
            case TYPE_EXAMPLE: return "Examples";
            default: return type;
        }
    }
    
    public String getDisplayContext() {
        if (context != null && !context.trim().isEmpty()) {
            return context;
        }
        return "Linked note";
    }
    
    @Override
    public String toString() {
        return String.format("NoteLink{type='%s', target='%s', strength=%.2f}", 
                           type, 
                           targetNote != null ? targetNote.getTitle() : "null", 
                           strength);
    }
}