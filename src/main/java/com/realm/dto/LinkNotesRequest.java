package com.realm.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for linking two notes together.
 * Supports creating relationships between notes with optional context and metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LinkNotesRequest {
    
    @NotNull(message = "Target note ID is required")
    private String targetNoteId;
    
    @Pattern(regexp = "REFERENCES|SUPPORTS|CONTRADICTS|BUILDS_ON|RELATED_TO|INSPIRED_BY|CLARIFIES|QUESTION|ANSWER|EXAMPLE", 
             message = "Link type must be one of: REFERENCES, SUPPORTS, CONTRADICTS, BUILDS_ON, RELATED_TO, INSPIRED_BY, CLARIFIES, QUESTION, ANSWER, EXAMPLE")
    private String type = "REFERENCES";
    
    @Size(max = 500, message = "Context cannot exceed 500 characters")
    private String context;
    
    @DecimalMin(value = "0.0", message = "Strength must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Strength must be between 0.0 and 1.0")
    private Double strength = 1.0;
    
    @Pattern(regexp = "OUTGOING|INCOMING|BIDIRECTIONAL", 
             message = "Direction must be OUTGOING, INCOMING, or BIDIRECTIONAL")
    private String direction = "OUTGOING";
}