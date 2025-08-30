package com.realm.exception;

/**
 * Exception thrown when a requested note link is not found.
 */
public class NoteLinkNotFoundException extends RuntimeException {
    
    private final String linkId;
    private final String noteId;
    
    public NoteLinkNotFoundException(Long linkId, Long noteId) {
        super("Note link not found with ID: " + linkId + " for note: " + noteId);
        this.linkId = String.valueOf(linkId);
        this.noteId = String.valueOf(noteId);
    }
    
    public NoteLinkNotFoundException(String linkId, String noteId) {
        super("Note link not found with ID: " + linkId + " for note: " + noteId);
        this.linkId = linkId;
        this.noteId = noteId;
    }
    
    public NoteLinkNotFoundException(String message) {
        super(message);
        this.linkId = null;
        this.noteId = null;
    }
    
    public String getLinkId() {
        return linkId;
    }
    
    public String getNoteId() {
        return noteId;
    }
}