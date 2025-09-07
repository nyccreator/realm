package com.realm.controller;

import com.realm.dto.CreateNoteRequest;
import com.realm.dto.LinkNotesRequest;
import com.realm.dto.UpdateNoteRequest;
import com.realm.model.Note;
import com.realm.model.User;
import com.realm.repository.NoteRepository;
import com.realm.service.NoteService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Note management operations.
 * 
 * This controller provides comprehensive REST API endpoints for note management
 * including CRUD operations, graph relationship management, and search functionality.
 * All operations are secured with JWT authentication and user access control.
 * 
 * API Endpoints:
 * - GET /api/notes - Get all notes for current user
 * - GET /api/notes/{id} - Get specific note
 * - POST /api/notes - Create new note
 * - PUT /api/notes/{id} - Update existing note
 * - DELETE /api/notes/{id} - Delete note
 * - POST /api/notes/{id}/links - Link notes together
 * - DELETE /api/notes/{id}/links/{linkId} - Remove link
 * - GET /api/notes/{id}/links - Get linked notes
 * - GET /api/notes/{id}/backlinks - Get backlinks
 * - GET /api/notes/search - Search notes
 * - GET /api/notes/tags/{tag} - Get notes by tag
 * - GET /api/notes/status/{status} - Get notes by status
 * - GET /api/notes/favorites - Get favorite notes
 * - POST /api/notes/{id}/favorite - Toggle favorite status
 */
@RestController
@RequestMapping("/api/notes")
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
public class NoteController {
    
    @Autowired
    private NoteService noteService;
    
    // ============================================================================
    // BASIC CRUD OPERATIONS
    // ============================================================================
    
    /**
     * Get all notes for the current user
     * 
     * @param user The authenticated user
     * @return List of notes owned by the user
     */
    @GetMapping
    public ResponseEntity<List<Note>> getAllNotes(@AuthenticationPrincipal User user) {
        log.debug("Getting all notes for user: {}", user.getId());
        
        List<Note> notes = noteService.findAllByUser(user.getId());
        
        log.info("Retrieved {} notes for user {}", notes.size(), user.getId());
        return ResponseEntity.ok(notes);
    }
    
    /**
     * Get a specific note by ID
     * 
     * @param noteId The note ID
     * @param user The authenticated user
     * @return The requested note
     */
    @GetMapping("/{noteId}")
    public ResponseEntity<Note> getNote(@PathVariable String noteId, 
                                       @AuthenticationPrincipal User user) {
        log.debug("Getting note {} for user: {}", noteId, user.getId());
        
        Note note = noteService.findByIdAndUser(noteId, user.getId());
        
        log.info("Retrieved note {} for user {}", noteId, user.getId());
        return ResponseEntity.ok(note);
    }
    
    /**
     * Create a new note
     * 
     * @param request The note creation request
     * @param user The authenticated user
     * @return The created note
     */
    @PostMapping
    public ResponseEntity<Note> createNote(@RequestBody @Valid CreateNoteRequest request,
                                         @AuthenticationPrincipal User user) {
        log.debug("Creating note '{}' for user: {}", request.getTitle(), user.getId());
        
        Note note = noteService.createNote(
            request.getTitle(), 
            request.getContent(), 
            request.getTags(), 
            user
        );
        
        log.info("Created note {} for user {}", note.getId(), user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(note);
    }
    
    /**
     * Update an existing note
     * 
     * @param noteId The note ID to update
     * @param request The note update request
     * @param user The authenticated user
     * @return The updated note
     */
    @PutMapping("/{noteId}")
    public ResponseEntity<Note> updateNote(@PathVariable String noteId,
                                         @RequestBody @Valid UpdateNoteRequest request,
                                         @AuthenticationPrincipal User user) {
        log.debug("Updating note {} for user: {}", noteId, user.getId());
        
        Note note = noteService.updateNote(
            noteId, 
            request.getTitle(), 
            request.getContent(), 
            request.getTags(), 
            user.getId()
        );
        
        log.info("Updated note {} for user {}", noteId, user.getId());
        return ResponseEntity.ok(note);
    }
    
    /**
     * Delete a note
     * 
     * @param noteId The note ID to delete
     * @param user The authenticated user
     * @return No content response
     */
    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> deleteNote(@PathVariable String noteId,
                                         @AuthenticationPrincipal User user) {
        log.debug("Deleting note {} for user: {}", noteId, user.getId());
        
        noteService.deleteNote(noteId, user.getId());
        
        log.info("Deleted note {} for user {}", noteId, user.getId());
        return ResponseEntity.noContent().build();
    }
    
    // ============================================================================
    // GRAPH RELATIONSHIP OPERATIONS
    // ============================================================================
    
    /**
     * Link two notes together
     * 
     * @param noteId The source note ID
     * @param request The link creation request
     * @param user The authenticated user
     * @return The updated source note with the new link
     */
    @PostMapping("/{noteId}/links")
    public ResponseEntity<Note> linkNotes(@PathVariable String noteId,
                                        @RequestBody @Valid LinkNotesRequest request,
                                        @AuthenticationPrincipal User user) {
        log.debug("Linking note {} to {} for user: {}", 
                 noteId, request.getTargetNoteId(), user.getId());
        
        Note note = noteService.linkNotes(
            noteId, 
            request.getTargetNoteId(),
            request.getType(),
            request.getContext(),
            user.getId()
        );
        
        log.info("Linked note {} to {} for user {}", 
                noteId, request.getTargetNoteId(), user.getId());
        return ResponseEntity.ok(note);
    }
    
    /**
     * Remove a link between notes
     * 
     * @param noteId The source note ID
     * @param linkId The link ID to remove
     * @param user The authenticated user
     * @return No content response
     */
    @DeleteMapping("/{noteId}/links/{linkId}")
    public ResponseEntity<Void> removeLink(@PathVariable String noteId,
                                         @PathVariable String linkId,
                                         @AuthenticationPrincipal User user) {
        log.debug("Removing link {} from note {} for user: {}", 
                 linkId, noteId, user.getId());
        
        noteService.removeLink(noteId, linkId, user.getId());
        
        log.info("Removed link {} from note {} for user {}", 
                linkId, noteId, user.getId());
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Get notes linked by a specific note (outgoing links)
     * 
     * @param noteId The source note ID
     * @param user The authenticated user
     * @return List of linked notes
     */
    @GetMapping("/{noteId}/links")
    public ResponseEntity<List<Note>> getLinkedNotes(@PathVariable String noteId,
                                                   @AuthenticationPrincipal User user) {
        log.debug("Getting linked notes for note {} and user: {}", noteId, user.getId());
        
        List<Note> linkedNotes = noteService.findLinkedNotes(noteId, user.getId());
        
        log.info("Retrieved {} linked notes for note {} and user {}", 
                linkedNotes.size(), noteId, user.getId());
        return ResponseEntity.ok(linkedNotes);
    }
    
    /**
     * Get notes that link to a specific note (backlinks)
     * 
     * @param noteId The target note ID
     * @param user The authenticated user
     * @return List of notes that link to the target note
     */
    @GetMapping("/{noteId}/backlinks")
    public ResponseEntity<List<Note>> getBacklinks(@PathVariable String noteId,
                                                 @AuthenticationPrincipal User user) {
        log.debug("Getting backlinks for note {} and user: {}", noteId, user.getId());
        
        List<Note> backlinks = noteService.findBacklinks(noteId, user.getId());
        
        log.info("Retrieved {} backlinks for note {} and user {}", 
                backlinks.size(), noteId, user.getId());
        return ResponseEntity.ok(backlinks);
    }
    
    /**
     * Get related notes using graph traversal
     * 
     * @param noteId The source note ID
     * @param depth The traversal depth (default: 2, max: 5)
     * @param limit The result limit (default: 20, max: 100)
     * @param user The authenticated user
     * @return List of related notes
     */
    @GetMapping("/{noteId}/related")
    public ResponseEntity<List<Note>> getRelatedNotes(@PathVariable String noteId,
                                                    @RequestParam(defaultValue = "2") int depth,
                                                    @RequestParam(defaultValue = "20") int limit,
                                                    @AuthenticationPrincipal User user) {
        log.debug("Getting related notes for note {} (depth={}, limit={}) for user: {}", 
                 noteId, depth, limit, user.getId());
        
        List<Note> relatedNotes = noteService.findRelatedNotes(noteId, user.getId(), depth, limit);
        
        log.info("Retrieved {} related notes for note {} and user {}", 
                relatedNotes.size(), noteId, user.getId());
        return ResponseEntity.ok(relatedNotes);
    }
    
    // ============================================================================
    // SEARCH AND DISCOVERY OPERATIONS
    // ============================================================================
    
    /**
     * Search notes by content
     * 
     * @param query The search query
     * @param user The authenticated user
     * @return List of matching notes
     */
    @GetMapping("/search")
    public ResponseEntity<List<Note>> searchNotes(@RequestParam String query,
                                                @AuthenticationPrincipal User user) {
        log.debug("Searching notes for query '{}' for user: {}", query, user.getId());
        
        List<Note> notes = noteService.searchNotes(query, user.getId());
        
        log.info("Found {} notes for query '{}' and user {}", 
                notes.size(), query, user.getId());
        return ResponseEntity.ok(notes);
    }
    
    /**
     * Get notes by tag
     * 
     * @param tag The tag to filter by
     * @param user The authenticated user
     * @return List of notes with the specified tag
     */
    @GetMapping("/tags/{tag}")
    public ResponseEntity<List<Note>> getNotesByTag(@PathVariable String tag,
                                                  @AuthenticationPrincipal User user) {
        log.debug("Getting notes with tag '{}' for user: {}", tag, user.getId());
        
        List<Note> notes = noteService.findByTag(tag, user.getId());
        
        log.info("Found {} notes with tag '{}' for user {}", 
                notes.size(), tag, user.getId());
        return ResponseEntity.ok(notes);
    }
    
    /**
     * Get notes by status
     * 
     * @param status The status to filter by (DRAFT, PUBLISHED, ARCHIVED)
     * @param user The authenticated user
     * @return List of notes with the specified status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Note>> getNotesByStatus(@PathVariable String status,
                                                     @AuthenticationPrincipal User user) {
        log.debug("Getting notes with status '{}' for user: {}", status, user.getId());
        
        List<Note> notes = noteService.findByStatus(status, user.getId());
        
        log.info("Found {} notes with status '{}' for user {}", 
                notes.size(), status, user.getId());
        return ResponseEntity.ok(notes);
    }
    
    /**
     * Get favorite notes
     * 
     * @param user The authenticated user
     * @return List of favorite notes
     */
    @GetMapping("/favorites")
    public ResponseEntity<List<Note>> getFavoriteNotes(@AuthenticationPrincipal User user) {
        log.debug("Getting favorite notes for user: {}", user.getId());
        
        List<Note> notes = noteService.findFavoriteNotes(user.getId());
        
        log.info("Found {} favorite notes for user {}", notes.size(), user.getId());
        return ResponseEntity.ok(notes);
    }
    
    /**
     * Get recently updated notes
     * 
     * @param days Number of days to look back (default: 7, max: 365)
     * @param user The authenticated user
     * @return List of recently updated notes
     */
    @GetMapping("/recent")
    public ResponseEntity<List<Note>> getRecentlyUpdatedNotes(
            @RequestParam(defaultValue = "7") int days,
            @AuthenticationPrincipal User user) {
        log.debug("Getting recently updated notes (last {} days) for user: {}", days, user.getId());
        
        List<Note> notes = noteService.findRecentlyUpdatedNotes(user.getId(), days);
        
        log.info("Found {} recently updated notes for user {}", notes.size(), user.getId());
        return ResponseEntity.ok(notes);
    }
    
    // ============================================================================
    // UTILITY OPERATIONS
    // ============================================================================
    
    /**
     * Toggle favorite status of a note
     * 
     * @param noteId The note ID
     * @param user The authenticated user
     * @return The updated note
     */
    @PostMapping("/{noteId}/favorite")
    public ResponseEntity<Note> toggleFavorite(@PathVariable String noteId,
                                             @AuthenticationPrincipal User user) {
        log.debug("Toggling favorite status for note {} and user: {}", noteId, user.getId());
        
        Note note = noteService.toggleFavorite(noteId, user.getId());
        
        log.info("Toggled favorite status for note {} to {} for user {}", 
                noteId, note.isFavorite(), user.getId());
        return ResponseEntity.ok(note);
    }
    
    /**
     * Update note status
     * 
     * @param noteId The note ID
     * @param statusRequest Map containing the new status
     * @param user The authenticated user
     * @return The updated note
     */
    @PatchMapping("/{noteId}/status")
    public ResponseEntity<Note> updateStatus(@PathVariable String noteId,
                                           @RequestBody Map<String, String> statusRequest,
                                           @AuthenticationPrincipal User user) {
        log.debug("Updating status for note {} for user: {}", noteId, user.getId());
        
        String status = statusRequest.get("status");
        Note note = noteService.updateStatus(noteId, status, user.getId());
        
        log.info("Updated status for note {} to '{}' for user {}", 
                noteId, status, user.getId());
        return ResponseEntity.ok(note);
    }
    
    /**
     * Get note statistics for the user
     * 
     * @param user The authenticated user
     * @return Map containing note statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<NoteRepository.NoteStatistics> getNoteStatistics(
            @AuthenticationPrincipal User user) {
        log.debug("Getting note statistics for user: {}", user.getId());
        
        NoteRepository.NoteStatistics statistics = noteService.getNoteStatistics(user.getId());
        
        log.info("Retrieved note statistics for user {}", user.getId());
        return ResponseEntity.ok(statistics);
    }
}