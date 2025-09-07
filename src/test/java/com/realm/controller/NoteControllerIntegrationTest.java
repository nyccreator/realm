package com.realm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realm.model.Note;
import com.realm.model.User;
import com.realm.repository.NoteRepository;
import com.realm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for NoteController.
 * 
 * These tests verify the complete REST API functionality including
 * HTTP request/response handling, JSON serialization, authentication,
 * and error responses.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
public class NoteControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private NoteRepository noteRepository;
    
    // JwtTokenProvider removed - using Redis session-based authentication
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private User testUser;
    private String authToken;
    
    @BeforeEach
    void setUp() {
        // Clean up database
        noteRepository.deleteAll();
        userRepository.deleteAll();
        
        // Create test user
        testUser = User.builder()
                .email("test@example.com")
                .passwordHash(passwordEncoder.encode("Test123@"))
                .displayName("Test User")
                .build();
        testUser = userRepository.save(testUser);
        
        // Session-based authentication - token not needed for testing
        authToken = "not-used-for-session-auth";
    }
    
    // ============================================================================
    // CRUD OPERATION TESTS
    // ============================================================================
    
    @Test
    @WithMockUser(username = "test@example.com")
    void shouldCreateNoteSuccessfully() throws Exception {
        // Given
        Map<String, Object> createRequest = Map.of(
            "title", "Test Note",
            "content", "This is test content",
            "tags", List.of("test", "integration")
        );
        
        // When & Then
        mockMvc.perform(post("/api/notes"))
                // Session-based auth - use Spring Security's test annotations
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test Note"))
                .andExpect(jsonPath("$.content").value("This is test content"))
                .andExpect(jsonPath("$.tags").isArray())
                .andExpect(jsonPath("$.tags[0]").value("test"))
                .andExpect(jsonPath("$.tags[1]").value("integration"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }
    
    @Test
    @WithMockUser(username = "test@example.com")
    void shouldGetAllNotesForUser() throws Exception {
        // Given
        Note note1 = Note.builder()
                .title("Note 1")
                .content("Content 1")
                .createdBy(testUser)
                .build();
        Note note2 = Note.builder()
                .title("Note 2")
                .content("Content 2")
                .createdBy(testUser)
                .build();
        
        noteRepository.save(note1);
        noteRepository.save(note2);
        
        // When & Then
        mockMvc.perform(get("/api/notes"))
                // Session-based auth - use Spring Security's test annotations
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").exists())
                .andExpect(jsonPath("$[1].title").exists());
    }
    
    @Test
    @WithMockUser(username = "test@example.com")
    void shouldGetSpecificNote() throws Exception {
        // Given
        Note note = Note.builder()
                .title("Test Note")
                .content("Test Content")
                .createdBy(testUser)
                .build();
        note = noteRepository.save(note);
        
        // When & Then
        mockMvc.perform(get("/api/notes/{noteId}", note.getId()))
                // Session-based auth - use Spring Security's test annotations
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(note.getId()))
                .andExpect(jsonPath("$.title").value("Test Note"))
                .andExpect(jsonPath("$.content").value("Test Content"));
    }
    
    @Test
    @WithMockUser(username = "test@example.com")
    void shouldUpdateNoteSuccessfully() throws Exception {
        // Given
        Note note = Note.builder()
                .title("Original Title")
                .content("Original Content")
                .createdBy(testUser)
                .build();
        note = noteRepository.save(note);
        
        Map<String, Object> updateRequest = Map.of(
            "title", "Updated Title",
            "content", "Updated Content",
            "tags", List.of("updated")
        );
        
        // When & Then
        mockMvc.perform(put("/api/notes/{noteId}", note.getId()))
                // Session-based auth - use Spring Security's test annotations
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.content").value("Updated Content"))
                .andExpect(jsonPath("$.tags[0]").value("updated"));
    }
    
    @Test
    @WithMockUser(username = "test@example.com")
    void shouldDeleteNoteSuccessfully() throws Exception {
        // Given
        Note note = Note.builder()
                .title("Note to Delete")
                .content("Content")
                .createdBy(testUser)
                .build();
        note = noteRepository.save(note);
        
        // When & Then
        mockMvc.perform(delete("/api/notes/{noteId}", note.getId()))
                // Session-based auth - use Spring Security's test annotations
                .andDo(print())
                .andExpect(status().isNoContent());
        
        // Verify note is deleted
        mockMvc.perform(get("/api/notes/{noteId}", note.getId()))
                // Session-based auth - use Spring Security's test annotations
                .andExpect(status().isNotFound());
    }
    
    // ============================================================================
    // RELATIONSHIP OPERATION TESTS
    // ============================================================================
    
    @Test
    @WithMockUser(username = "test@example.com")
    void shouldLinkNotesSuccessfully() throws Exception {
        // Given
        Note sourceNote = Note.builder()
                .title("Source Note")
                .content("Source Content")
                .createdBy(testUser)
                .build();
        Note targetNote = Note.builder()
                .title("Target Note")
                .content("Target Content")
                .createdBy(testUser)
                .build();
        
        sourceNote = noteRepository.save(sourceNote);
        targetNote = noteRepository.save(targetNote);
        
        Map<String, Object> linkRequest = Map.of(
            "targetNoteId", targetNote.getId(),
            "type", "REFERENCES",
            "context", "This references that"
        );
        
        // When & Then
        mockMvc.perform(post("/api/notes/{noteId}/links", sourceNote.getId())
                // Session-based auth - use Spring Security's test annotations
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(linkRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outgoingLinks").isArray())
                .andExpect(jsonPath("$.outgoingLinks[0].targetNote.id").value(targetNote.getId()))
                .andExpect(jsonPath("$.outgoingLinks[0].type").value("REFERENCES"))
                .andExpect(jsonPath("$.outgoingLinks[0].context").value("This references that"));
    }
    
    @Test
    @WithMockUser(username = "test@example.com")
    void shouldGetLinkedNotes() throws Exception {
        // Given - create notes with links through service layer
        Note sourceNote = Note.builder()
                .title("Source")
                .content("Content")
                .createdBy(testUser)
                .build();
        Note targetNote = Note.builder()
                .title("Target")
                .content("Content")
                .createdBy(testUser)
                .build();
        
        sourceNote = noteRepository.save(sourceNote);
        targetNote = noteRepository.save(targetNote);
        
        // When & Then
        mockMvc.perform(get("/api/notes/{noteId}/links", sourceNote.getId()))
                // Session-based auth - use Spring Security's test annotations
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
    
    // ============================================================================
    // SEARCH OPERATION TESTS
    // ============================================================================
    
    @Test
    @WithMockUser(username = "test@example.com")
    void shouldSearchNotesSuccessfully() throws Exception {
        // Given
        Note note = Note.builder()
                .title("Java Programming")
                .content("Learn about Java streams and lambdas")
                .createdBy(testUser)
                .build();
        noteRepository.save(note);
        
        // When & Then
        mockMvc.perform(get("/api/notes/search")
                .param("query", "Java"))
                // Session-based auth - use Spring Security's test annotations
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
    
    @Test
    @WithMockUser(username = "test@example.com")
    void shouldGetNotesByTag() throws Exception {
        // Given
        Note note = Note.builder()
                .title("Tagged Note")
                .content("Content")
                .tags(List.of("programming", "java"))
                .createdBy(testUser)
                .build();
        noteRepository.save(note);
        
        // When & Then
        mockMvc.perform(get("/api/notes/tags/{tag}", "programming"))
                // Session-based auth - use Spring Security's test annotations
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
    
    @Test
    @WithMockUser(username = "test@example.com")
    void shouldToggleFavoriteStatus() throws Exception {
        // Given
        Note note = Note.builder()
                .title("Note to Favorite")
                .content("Content")
                .createdBy(testUser)
                .build();
        note = noteRepository.save(note);
        
        // When & Then
        mockMvc.perform(post("/api/notes/{noteId}/favorite", note.getId()))
                // Session-based auth - use Spring Security's test annotations
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFavorite").value(true));
    }
    
    // ============================================================================
    // ERROR HANDLING TESTS
    // ============================================================================
    
    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/notes"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    @WithMockUser(username = "test@example.com")
    void shouldReturn400WhenCreateRequestInvalid() throws Exception {
        // Given - invalid request with empty title
        Map<String, Object> invalidRequest = Map.of(
            "title", "",
            "content", "Content"
        );
        
        // When & Then
        mockMvc.perform(post("/api/notes")
                // Session-based auth - use Spring Security's test annotations
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }
    
    @Test
    @WithMockUser(username = "test@example.com")
    void shouldReturn404WhenNoteNotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/notes/{noteId}", "non-existent-id"))
                // Session-based auth - use Spring Security's test annotations
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Note Not Found"));
    }
    
    @Test
    @WithMockUser(username = "test@example.com")
    void shouldReturn400WhenSearchQueryTooShort() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/notes/search")
                .param("query", "x"))
                // Session-based auth - use Spring Security's test annotations
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"));
    }
    
    @Test
    @WithMockUser(username = "test@example.com")
    void shouldReturn400WhenLinkingNoteToItself() throws Exception {
        // Given
        Note note = Note.builder()
                .title("Self Link Test")
                .content("Content")
                .createdBy(testUser)
                .build();
        note = noteRepository.save(note);
        
        Map<String, Object> linkRequest = Map.of(
            "targetNoteId", note.getId(),
            "type", "REFERENCES"
        );
        
        // When & Then
        mockMvc.perform(post("/api/notes/{noteId}/links", note.getId())
                // Session-based auth - use Spring Security's test annotations
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(linkRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.message").value("Cannot link a note to itself"));
    }
}