package com.realm.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Note entity representing individual knowledge items in the PKM system.
 * This is the core content node in our graph-based knowledge management system.
 * 
 * Graph Design: Notes are connected to Users via CREATED_BY relationships and to other Notes
 * via REFERENCES relationships through NoteLink properties. This creates a rich knowledge graph
 * where ideas, concepts, and information can be interconnected and traversed efficiently.
 */
@Node("Note")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Note {
    
    @Id 
    @GeneratedValue
    private Long id;
    
    @Property("title")
    @NotBlank(message = "Note title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;
    
    @Property("content")
    private String content; // Rich text JSON from TipTap editor
    
    @Property("summary")
    @Size(max = 500, message = "Summary cannot exceed 500 characters")
    private String summary; // Auto-generated or user-provided summary
    
    @Property("tags")
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    
    @Property("category")
    private String category; // Optional categorization
    
    @Property("status")
    @Builder.Default
    private String status = "DRAFT"; // DRAFT, PUBLISHED, ARCHIVED
    
    @Property("priority")
    @Builder.Default
    private String priority = "NORMAL"; // LOW, NORMAL, HIGH, URGENT
    
    @Property("isPublic")
    @Builder.Default
    private boolean isPublic = false; // For future multi-user features
    
    @Property("isFavorite")
    @Builder.Default
    private boolean isFavorite = false;
    
    // Metadata removed for simplicity - can be added back later with proper JSON serialization
    
    @Property("wordCount")
    @Builder.Default
    private int wordCount = 0;
    
    @Property("readingTime")
    @Builder.Default
    private int readingTime = 0; // Estimated reading time in minutes
    
    @Property("createdAt")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Property("updatedAt")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Property("lastAccessedAt")
    private LocalDateTime lastAccessedAt;
    
    // Graph relationships - the core of our PKM system
    @Relationship(type = "CREATED_BY", direction = Relationship.Direction.OUTGOING)
    private User createdBy;
    
    @Relationship(type = "REFERENCES", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<NoteLink> outgoingLinks = new HashSet<>();
    
    // Incoming links are handled by querying the graph rather than storing bidirectional relationships
    // This approach is more performance-friendly for Neo4j
    
    // Constructors
    public Note(String title, String content, User createdBy) {
        this();
        this.title = title;
        this.content = content;
        this.createdBy = createdBy;
        updateMetrics();
    }
    
    // Utility methods for note management
    public void updateContent(String newTitle, String newContent) {
        if (newTitle != null && !newTitle.trim().isEmpty()) {
            this.title = newTitle.trim();
        }
        if (newContent != null) {
            this.content = newContent;
        }
        this.updatedAt = LocalDateTime.now();
        updateMetrics();
    }
    
    public void addTag(String tag) {
        if (tag != null && !tag.trim().isEmpty()) {
            String normalizedTag = tag.trim().toLowerCase();
            if (!tags.contains(normalizedTag)) {
                tags.add(normalizedTag);
                this.updatedAt = LocalDateTime.now();
            }
        }
    }
    
    public void removeTag(String tag) {
        if (tag != null) {
            tags.remove(tag.trim().toLowerCase());
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    public void addTags(List<String> newTags) {
        if (newTags != null) {
            for (String tag : newTags) {
                addTag(tag);
            }
        }
    }
    
    public void setTags(List<String> newTags) {
        this.tags.clear();
        if (newTags != null) {
            addTags(newTags);
        }
    }
    
    // Metadata methods removed - can be added back later with proper JSON serialization
    
    public void markAsAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }
    
    public void toggleFavorite() {
        this.isFavorite = !this.isFavorite;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updateStatus(String newStatus) {
        if (Arrays.asList("DRAFT", "PUBLISHED", "ARCHIVED").contains(newStatus)) {
            this.status = newStatus;
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    public void updatePriority(String newPriority) {
        if (Arrays.asList("LOW", "NORMAL", "HIGH", "URGENT").contains(newPriority)) {
            this.priority = newPriority;
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    // Calculate metrics based on content
    private void updateMetrics() {
        if (content != null && !content.trim().isEmpty()) {
            // Simple word count (can be enhanced for rich text parsing)
            String plainText = content.replaceAll("<[^>]*>", ""); // Strip HTML tags
            String[] words = plainText.trim().split("\\s+");
            this.wordCount = words.length;
            
            // Estimate reading time (average 200 words per minute)
            this.readingTime = Math.max(1, wordCount / 200);
        } else {
            this.wordCount = 0;
            this.readingTime = 0;
        }
    }
    
    // Helper method to check if note contains search term
    public boolean containsSearchTerm(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return false;
        }
        
        String term = searchTerm.toLowerCase();
        return (title != null && title.toLowerCase().contains(term)) ||
               (content != null && content.toLowerCase().contains(term)) ||
               (summary != null && summary.toLowerCase().contains(term)) ||
               tags.stream().anyMatch(tag -> tag.toLowerCase().contains(term));
    }
    
    // Get display summary
    public String getDisplaySummary() {
        if (summary != null && !summary.trim().isEmpty()) {
            return summary;
        }
        
        if (content != null && !content.trim().isEmpty()) {
            // Create summary from content (first 150 characters)
            String plainText = content.replaceAll("<[^>]*>", "").trim();
            if (plainText.length() > 150) {
                return plainText.substring(0, 147) + "...";
            }
            return plainText;
        }
        
        return "No content";
    }
}