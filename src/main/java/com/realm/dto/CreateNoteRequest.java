package com.realm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a new note.
 * Contains validation rules for note creation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateNoteRequest {
    
    @NotBlank(message = "Note title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;
    
    private String content; // Rich text JSON from TipTap editor
    
    @Size(max = 500, message = "Summary cannot exceed 500 characters")
    private String summary;
    
    private List<String> tags;
    
    private String category;
    
    @Pattern(regexp = "DRAFT|PUBLISHED|ARCHIVED", message = "Status must be DRAFT, PUBLISHED, or ARCHIVED")
    private String status = "DRAFT";
    
    @Pattern(regexp = "LOW|NORMAL|HIGH|URGENT", message = "Priority must be LOW, NORMAL, HIGH, or URGENT")
    private String priority = "NORMAL";
    
    private boolean isPublic = false;
    
    private boolean isFavorite = false;
}