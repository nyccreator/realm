package com.realm.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * NoteVersion - Represents a historical version of a note
 * 
 * Enables version control, change tracking, and content history
 * for advanced note management and collaboration features.
 */
@Node("NoteVersion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoteVersion {
    
    @Id
    @Builder.Default
    private String id = UUID.randomUUID().toString();
    
    @Property("versionNumber")
    private Integer versionNumber;
    
    @Property("title")
    private String title;
    
    @Property("content")
    private String content;
    
    @Property("tags")
    @Builder.Default
    private List<String> tags = List.of();
    
    @Property("wordCount")
    private Integer wordCount;
    
    @Property("summary")
    private String summary;
    
    @Property("changeDescription")
    private String changeDescription; // Optional description of what changed
    
    @Property("createdAt")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Property("contentHash")
    private String contentHash; // Hash to quickly detect content changes
    
    // Relationships
    @Relationship(type = "VERSION_OF", direction = Relationship.Direction.OUTGOING)
    private Note originalNote;
    
    @Relationship(type = "CREATED_BY", direction = Relationship.Direction.OUTGOING)
    private User createdBy;
    
    /**
     * Create a version from a current note
     */
    public static NoteVersion fromNote(Note note) {
        return NoteVersion.builder()
            .title(note.getTitle())
            .content(note.getContent())
            .tags(note.getTags())
            .wordCount(note.getWordCount())
            .summary(note.getSummary())
            .originalNote(note)
            .createdBy(note.getCreatedBy())
            .contentHash(calculateContentHash(note))
            .build();
    }
    
    /**
     * Calculate content hash for change detection
     */
    private static String calculateContentHash(Note note) {
        String combined = (note.getTitle() != null ? note.getTitle() : "") +
                         (note.getContent() != null ? note.getContent() : "") +
                         String.join(",", note.getTags());
        
        return Integer.toHexString(combined.hashCode());
    }
    
    /**
     * Check if this version has significant changes from another
     */
    public boolean hasSignificantChanges(NoteVersion other) {
        if (other == null) return true;
        
        // Compare content hashes first (fast check)
        if (!this.contentHash.equals(other.contentHash)) {
            return true;
        }
        
        // Additional checks for edge cases
        return !this.title.equals(other.title) ||
               !this.content.equals(other.content) ||
               !this.tags.equals(other.tags);
    }
}