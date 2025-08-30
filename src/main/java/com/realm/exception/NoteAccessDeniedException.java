package com.realm.exception;

/**
 * Exception thrown when a user tries to access a note they don't own.
 */
public class NoteAccessDeniedException extends RuntimeException {
    
    private final String noteId;
    private final String userId;
    
    public NoteAccessDeniedException(Long noteId, Long userId) {
        super("Access denied to note ID: " + noteId + " for user: " + userId);
        this.noteId = String.valueOf(noteId);
        this.userId = String.valueOf(userId);
    }
    
    public NoteAccessDeniedException(String noteId, String userId) {
        super("Access denied to note ID: " + noteId + " for user: " + userId);
        this.noteId = noteId;
        this.userId = userId;
    }
    
    public NoteAccessDeniedException(String message) {
        super(message);
        this.noteId = null;
        this.userId = null;
    }
    
    public String getNoteId() {
        return noteId;
    }
    
    public String getUserId() {
        return userId;
    }
}