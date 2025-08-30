package com.realm.service;

import com.realm.exception.NoteAccessDeniedException;
import com.realm.exception.NoteLinkNotFoundException;
import com.realm.exception.NoteNotFoundException;
import com.realm.exception.ValidationException;
import com.realm.model.Note;
import com.realm.model.NoteLink;
import com.realm.model.User;
import com.realm.repository.NoteRepository;
import com.realm.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * NoteService provides business logic for note management operations.
 * 
 * This service implements the core note management functionality including
 * CRUD operations, graph relationship management, and search capabilities.
 * It follows graph-first design principles and integrates seamlessly with
 * the Neo4j database through optimized queries.
 * 
 * Features:
 * - Complete note CRUD operations with user access control
 * - Graph relationship management (note linking and unlinking)
 * - Search and discovery functionality
 * - User permission validation
 * - Comprehensive error handling and logging
 */
@Service
@Transactional
@Slf4j
public class NoteService {
    
    @Autowired
    private NoteRepository noteRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    // ============================================================================
    // BASIC CRUD OPERATIONS
    // ============================================================================
    
    /**
     * Find all notes for a specific user
     */
    public List<Note> findAllByUser(Long userId) {
        log.debug("Finding all notes for user: {}", userId);
        return noteRepository.findByCreatedByUserId(userId);
    }
    
    /**
     * Find a specific note by ID and verify user access
     */
    public Note findByIdAndUser(Long noteId, Long userId) {
        log.debug("Finding note {} for user: {}", noteId, userId);
        
        Optional<Note> noteOpt = noteRepository.findById(noteId);
        if (noteOpt.isEmpty()) {
            throw new NoteNotFoundException(noteId);
        }
        
        Note note = noteOpt.get();
        if (!isNoteAccessibleToUser(note, userId)) {
            throw new NoteAccessDeniedException(noteId, userId);
        }
        
        // Update last accessed timestamp
        note.markAsAccessed();
        noteRepository.save(note);
        
        return note;
    }
    
    /**
     * Create a new note
     */
    public Note createNote(String title, String content, List<String> tags, User user) {
        log.debug("Creating new note '{}' for user: {}", title, user.getId());
        
        validateNoteInput(title, content);
        
        Note note = Note.builder()
                .title(title.trim())
                .content(content)
                .tags(tags != null ? tags : List.of())
                .createdBy(user)
                .build();
        
        Note savedNote = noteRepository.save(note);
        log.info("Created note {} for user {}", savedNote.getId(), user.getId());
        
        return savedNote;
    }
    
    /**
     * Update an existing note
     */
    public Note updateNote(Long noteId, String title, String content, List<String> tags, Long userId) {
        log.debug("Updating note {} for user: {}", noteId, userId);
        
        Note note = findByIdAndUser(noteId, userId);
        
        // Update fields if provided
        if (title != null && !title.trim().isEmpty()) {
            validateTitle(title);
            note.setTitle(title.trim());
        }
        
        if (content != null) {
            note.setContent(content);
        }
        
        if (tags != null) {
            note.setTags(tags);
        }
        
        note.setUpdatedAt(LocalDateTime.now());
        
        Note updatedNote = noteRepository.save(note);
        log.info("Updated note {} for user {}", noteId, userId);
        
        return updatedNote;
    }
    
    /**
     * Delete a note
     */
    public void deleteNote(Long noteId, Long userId) {
        log.debug("Deleting note {} for user: {}", noteId, userId);
        
        Note note = findByIdAndUser(noteId, userId);
        
        noteRepository.delete(note);
        log.info("Deleted note {} for user {}", noteId, userId);
    }
    
    // ============================================================================
    // GRAPH RELATIONSHIP OPERATIONS
    // ============================================================================
    
    /**
     * Link two notes together
     */
    public Note linkNotes(Long sourceNoteId, Long targetNoteId, String type, String context, Long userId) {
        log.debug("Linking notes {} -> {} with type '{}' for user: {}", 
                 sourceNoteId, targetNoteId, type, userId);
        
        // Validate both notes belong to the user
        Note sourceNote = findByIdAndUser(sourceNoteId, userId);
        Note targetNote = findByIdAndUser(targetNoteId, userId);
        
        // Prevent self-linking
        if (sourceNoteId.equals(targetNoteId)) {
            throw new ValidationException("Cannot link a note to itself");
        }
        
        // Validate link type
        if (type != null && !NoteLink.isValidType(type)) {
            throw new ValidationException("Invalid link type: " + type);
        }
        
        // Check if link already exists
        boolean linkExists = sourceNote.getOutgoingLinks().stream()
                .anyMatch(link -> link.getTargetNote().getId().equals(targetNoteId));
        
        if (linkExists) {
            throw new ValidationException("Link already exists between these notes");
        }
        
        // Create the link
        NoteLink noteLink = NoteLink.builder()
                .targetNote(targetNote)
                .type(type != null ? type : NoteLink.TYPE_REFERENCES)
                .context(context)
                .build();
        
        sourceNote.getOutgoingLinks().add(noteLink);
        sourceNote.setUpdatedAt(LocalDateTime.now());
        
        Note updatedNote = noteRepository.save(sourceNote);
        log.info("Linked notes {} -> {} for user {}", sourceNoteId, targetNoteId, userId);
        
        return updatedNote;
    }
    
    /**
     * Remove a link between notes
     */
    public void removeLink(Long sourceNoteId, Long linkId, Long userId) {
        log.debug("Removing link {} from note {} for user: {}", linkId, sourceNoteId, userId);
        
        Note sourceNote = findByIdAndUser(sourceNoteId, userId);
        
        boolean linkRemoved = sourceNote.getOutgoingLinks().removeIf(link -> 
            link.getId() != null && link.getId().equals(linkId));
        
        if (!linkRemoved) {
            throw new NoteLinkNotFoundException(linkId, sourceNoteId);
        }
        
        sourceNote.setUpdatedAt(LocalDateTime.now());
        noteRepository.save(sourceNote);
        
        log.info("Removed link {} from note {} for user {}", linkId, sourceNoteId, userId);
    }
    
    /**
     * Find notes linked by a specific note (outgoing links)
     */
    public List<Note> findLinkedNotes(Long noteId, Long userId) {
        log.debug("Finding linked notes for note {} and user: {}", noteId, userId);
        
        // Verify user has access to the note
        findByIdAndUser(noteId, userId);
        
        return noteRepository.findNotesReferencedByNote(noteId, userId);
    }
    
    /**
     * Find notes that link to a specific note (backlinks)
     */
    public List<Note> findBacklinks(Long noteId, Long userId) {
        log.debug("Finding backlinks for note {} and user: {}", noteId, userId);
        
        // Verify user has access to the note
        findByIdAndUser(noteId, userId);
        
        return noteRepository.findNotesReferencingNote(noteId, userId);
    }
    
    /**
     * Find related notes using graph traversal
     */
    public List<Note> findRelatedNotes(Long noteId, Long userId, int depth, int limit) {
        log.debug("Finding related notes for note {} (depth={}, limit={}) for user: {}", 
                 noteId, depth, limit, userId);
        
        // Verify user has access to the note
        findByIdAndUser(noteId, userId);
        
        // Validate parameters
        if (depth < 1 || depth > 5) {
            throw new ValidationException("Depth must be between 1 and 5");
        }
        if (limit < 1 || limit > 100) {
            throw new ValidationException("Limit must be between 1 and 100");
        }
        
        return noteRepository.findRelatedNotes(noteId, userId, depth, limit);
    }
    
    // ============================================================================
    // SEARCH AND DISCOVERY OPERATIONS
    // ============================================================================
    
    /**
     * Search notes by content using full-text search
     */
    public List<Note> searchNotes(String query, Long userId) {
        log.debug("Searching notes for query '{}' for user: {}", query, userId);
        
        if (query == null || query.trim().isEmpty()) {
            throw new ValidationException("Search query cannot be empty");
        }
        
        String searchTerm = query.trim();
        if (searchTerm.length() < 2) {
            throw new ValidationException("Search query must be at least 2 characters long");
        }
        
        return noteRepository.searchNotesByContent(userId, searchTerm);
    }
    
    /**
     * Find notes by tag
     */
    public List<Note> findByTag(String tag, Long userId) {
        log.debug("Finding notes with tag '{}' for user: {}", tag, userId);
        
        if (tag == null || tag.trim().isEmpty()) {
            throw new ValidationException("Tag cannot be empty");
        }
        
        return noteRepository.findByCreatedByUserIdAndTag(userId, tag.trim().toLowerCase());
    }
    
    /**
     * Find notes by status
     */
    public List<Note> findByStatus(String status, Long userId) {
        log.debug("Finding notes with status '{}' for user: {}", status, userId);
        
        if (!List.of("DRAFT", "PUBLISHED", "ARCHIVED").contains(status)) {
            throw new ValidationException("Invalid status: " + status);
        }
        
        return noteRepository.findByCreatedByUserIdAndStatus(userId, status);
    }
    
    /**
     * Find favorite notes
     */
    public List<Note> findFavoriteNotes(Long userId) {
        log.debug("Finding favorite notes for user: {}", userId);
        return noteRepository.findFavoritesByUserId(userId);
    }
    
    /**
     * Find recently updated notes
     */
    public List<Note> findRecentlyUpdatedNotes(Long userId, int days) {
        log.debug("Finding recently updated notes (last {} days) for user: {}", days, userId);
        
        if (days < 1 || days > 365) {
            throw new ValidationException("Days must be between 1 and 365");
        }
        
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return noteRepository.findRecentlyUpdatedByUserId(userId, since);
    }
    
    // ============================================================================
    // UTILITY AND HELPER METHODS
    // ============================================================================
    
    /**
     * Toggle favorite status of a note
     */
    public Note toggleFavorite(Long noteId, Long userId) {
        log.debug("Toggling favorite status for note {} and user: {}", noteId, userId);
        
        Note note = findByIdAndUser(noteId, userId);
        note.toggleFavorite();
        
        Note updatedNote = noteRepository.save(note);
        log.info("Toggled favorite status for note {} to {} for user {}", 
                noteId, updatedNote.isFavorite(), userId);
        
        return updatedNote;
    }
    
    /**
     * Update note status
     */
    public Note updateStatus(Long noteId, String status, Long userId) {
        log.debug("Updating status of note {} to '{}' for user: {}", noteId, status, userId);
        
        Note note = findByIdAndUser(noteId, userId);
        note.updateStatus(status);
        
        Note updatedNote = noteRepository.save(note);
        log.info("Updated status of note {} to '{}' for user {}", noteId, status, userId);
        
        return updatedNote;
    }
    
    /**
     * Get note statistics for a user
     */
    public NoteRepository.NoteStatistics getNoteStatistics(Long userId) {
        log.debug("Getting note statistics for user: {}", userId);
        return noteRepository.getNoteStatistics(userId);
    }
    
    // ============================================================================
    // VALIDATION METHODS
    // ============================================================================
    
    private void validateNoteInput(String title, String content) {
        validateTitle(title);
        // Content can be empty (allows for title-only notes)
    }
    
    private void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new ValidationException("Note title is required");
        }
        if (title.length() > 200) {
            throw new ValidationException("Title cannot exceed 200 characters");
        }
    }
    
    private boolean isNoteAccessibleToUser(Note note, Long userId) {
        // For now, users can only access their own notes
        // This will be extended in future sections for multi-user sharing
        return note.getCreatedBy() != null && 
               note.getCreatedBy().getId().equals(userId);
    }
}