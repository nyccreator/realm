package com.realm.exception;

/**
 * Exception thrown when a requested note is not found.
 */
public class NoteNotFoundException extends RuntimeException {
    
    private final String noteId;
    private final String path;
    
    public NoteNotFoundException(Long noteId) {
        super("Note not found with ID: " + noteId);
        this.noteId = String.valueOf(noteId);
        this.path = "/api/notes/" + noteId;
    }
    
    public NoteNotFoundException(String noteId) {
        super("Note not found with ID: " + noteId);
        this.noteId = noteId;
        this.path = "/api/notes/" + noteId;
    }
    
    public NoteNotFoundException(String message, String path) {
        super(message);
        this.noteId = null;
        this.path = path;
    }
    
    public String getNoteId() {
        return noteId;
    }
    
    public String getPath() {
        return path;
    }
}