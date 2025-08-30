package com.realm.service;

import com.realm.exception.NoteAccessDeniedException;
import com.realm.exception.NoteNotFoundException;
import com.realm.exception.ValidationException;
import com.realm.model.Note;
import com.realm.model.User;
import com.realm.repository.NoteRepository;
import com.realm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for NoteService.
 * 
 * These tests verify the complete note management functionality including
 * CRUD operations, graph relationships, search, and error handling.
 * Tests run against the actual Neo4j database with test profile configuration.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class NoteServiceIntegrationTest {
    
    @Autowired
    private NoteService noteService;
    
    @Autowired
    private NoteRepository noteRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    private User testUser;
    private User otherUser;
    
    @BeforeEach
    void setUp() {
        // Clean up database
        noteRepository.deleteAll();
        userRepository.deleteAll();
        
        // Create test users
        testUser = User.builder()
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .displayName("Test User")
                .build();
        testUser = userRepository.save(testUser);
        
        otherUser = User.builder()
                .email("other@example.com")
                .passwordHash("hashedPassword")
                .displayName("Other User")
                .build();
        otherUser = userRepository.save(otherUser);
    }
    
    // ============================================================================
    // CRUD OPERATION TESTS
    // ============================================================================
    
    @Test
    void shouldCreateNoteSuccessfully() {
        // Given
        String title = "Test Note";
        String content = "This is test content";
        List<String> tags = List.of("test", "integration");
        
        // When
        Note createdNote = noteService.createNote(title, content, tags, testUser);
        
        // Then
        assertThat(createdNote).isNotNull();
        assertThat(createdNote.getId()).isNotNull();
        assertThat(createdNote.getTitle()).isEqualTo(title);
        assertThat(createdNote.getContent()).isEqualTo(content);
        assertThat(createdNote.getTags()).containsExactlyInAnyOrderElementsOf(tags);
        assertThat(createdNote.getCreatedBy().getId()).isEqualTo(testUser.getId());
        assertThat(createdNote.getCreatedAt()).isNotNull();
        assertThat(createdNote.getUpdatedAt()).isNotNull();
    }
    
    @Test
    void shouldFindAllNotesByUser() {
        // Given
        Note note1 = noteService.createNote("Note 1", "Content 1", List.of("tag1"), testUser);
        Note note2 = noteService.createNote("Note 2", "Content 2", List.of("tag2"), testUser);
        noteService.createNote("Other Note", "Other Content", List.of("tag3"), otherUser);
        
        // When
        List<Note> userNotes = noteService.findAllByUser(testUser.getId());
        
        // Then
        assertThat(userNotes).hasSize(2);
        assertThat(userNotes).extracting(Note::getId)
                .containsExactlyInAnyOrder(note1.getId(), note2.getId());
    }
    
    @Test
    void shouldFindNoteByIdAndUser() {
        // Given
        Note note = noteService.createNote("Test Note", "Test Content", null, testUser);
        
        // When
        Note foundNote = noteService.findByIdAndUser(note.getId(), testUser.getId());
        
        // Then
        assertThat(foundNote).isNotNull();
        assertThat(foundNote.getId()).isEqualTo(note.getId());
        assertThat(foundNote.getTitle()).isEqualTo("Test Note");
        assertThat(foundNote.getLastAccessedAt()).isNotNull();
    }
    
    @Test
    void shouldThrowExceptionWhenNoteNotFound() {
        // Given
        Long nonExistentNoteId = 999L;
        
        // When & Then
        assertThatThrownBy(() -> noteService.findByIdAndUser(nonExistentNoteId, testUser.getId()))
                .isInstanceOf(NoteNotFoundException.class)
                .hasMessageContaining("999");
    }
    
    @Test
    void shouldThrowExceptionWhenUserCannotAccessNote() {
        // Given
        Note note = noteService.createNote("Test Note", "Test Content", null, testUser);
        
        // When & Then
        assertThatThrownBy(() -> noteService.findByIdAndUser(note.getId(), otherUser.getId()))
                .isInstanceOf(NoteAccessDeniedException.class);
    }
    
    @Test
    void shouldUpdateNoteSuccessfully() {
        // Given
        Note note = noteService.createNote("Original Title", "Original Content", List.of("original"), testUser);
        String updatedTitle = "Updated Title";
        String updatedContent = "Updated Content";
        List<String> updatedTags = List.of("updated", "modified");
        
        // When
        Note updatedNote = noteService.updateNote(
                note.getId(), updatedTitle, updatedContent, updatedTags, testUser.getId());
        
        // Then
        assertThat(updatedNote.getId()).isEqualTo(note.getId());
        assertThat(updatedNote.getTitle()).isEqualTo(updatedTitle);
        assertThat(updatedNote.getContent()).isEqualTo(updatedContent);
        assertThat(updatedNote.getTags()).containsExactlyInAnyOrderElementsOf(updatedTags);
        assertThat(updatedNote.getUpdatedAt()).isAfter(note.getUpdatedAt());
    }
    
    @Test
    void shouldUpdateNotePartially() {
        // Given
        Note note = noteService.createNote("Original Title", "Original Content", List.of("original"), testUser);
        String updatedTitle = "Updated Title";
        
        // When (only update title)
        Note updatedNote = noteService.updateNote(
                note.getId(), updatedTitle, null, null, testUser.getId());
        
        // Then
        assertThat(updatedNote.getTitle()).isEqualTo(updatedTitle);
        assertThat(updatedNote.getContent()).isEqualTo("Original Content"); // unchanged
        assertThat(updatedNote.getTags()).containsExactly("original"); // unchanged
    }
    
    @Test
    void shouldDeleteNoteSuccessfully() {
        // Given
        Note note = noteService.createNote("Test Note", "Test Content", null, testUser);
        Long noteId = note.getId();
        
        // When
        noteService.deleteNote(noteId, testUser.getId());
        
        // Then
        assertThatThrownBy(() -> noteService.findByIdAndUser(noteId, testUser.getId()))
                .isInstanceOf(NoteNotFoundException.class);
    }
    
    // ============================================================================
    // GRAPH RELATIONSHIP TESTS
    // ============================================================================
    
    @Test
    void shouldLinkNotesSuccessfully() {
        // Given
        Note sourceNote = noteService.createNote("Source Note", "Source Content", null, testUser);
        Note targetNote = noteService.createNote("Target Note", "Target Content", null, testUser);
        String linkType = "REFERENCES";
        String context = "This references that";
        
        // When
        Note updatedNote = noteService.linkNotes(
                sourceNote.getId(), targetNote.getId(), linkType, context, testUser.getId());
        
        // Then
        assertThat(updatedNote.getOutgoingLinks()).hasSize(1);
        assertThat(updatedNote.getOutgoingLinks().iterator().next().getTargetNote().getId())
                .isEqualTo(targetNote.getId());
        assertThat(updatedNote.getOutgoingLinks().iterator().next().getType())
                .isEqualTo(linkType);
        assertThat(updatedNote.getOutgoingLinks().iterator().next().getContext())
                .isEqualTo(context);
    }
    
    @Test
    void shouldPreventSelfLinking() {
        // Given
        Note note = noteService.createNote("Test Note", "Test Content", null, testUser);
        
        // When & Then
        assertThatThrownBy(() -> noteService.linkNotes(
                note.getId(), note.getId(), "REFERENCES", null, testUser.getId()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot link a note to itself");
    }
    
    @Test
    void shouldPreventDuplicateLinks() {
        // Given
        Note sourceNote = noteService.createNote("Source Note", "Source Content", null, testUser);
        Note targetNote = noteService.createNote("Target Note", "Target Content", null, testUser);
        
        // Create first link
        noteService.linkNotes(sourceNote.getId(), targetNote.getId(), "REFERENCES", null, testUser.getId());
        
        // When & Then
        assertThatThrownBy(() -> noteService.linkNotes(
                sourceNote.getId(), targetNote.getId(), "REFERENCES", null, testUser.getId()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Link already exists");
    }
    
    @Test
    void shouldFindLinkedNotes() {
        // Given
        Note sourceNote = noteService.createNote("Source Note", "Source Content", null, testUser);
        Note target1 = noteService.createNote("Target 1", "Content 1", null, testUser);
        Note target2 = noteService.createNote("Target 2", "Content 2", null, testUser);
        
        noteService.linkNotes(sourceNote.getId(), target1.getId(), "REFERENCES", null, testUser.getId());
        noteService.linkNotes(sourceNote.getId(), target2.getId(), "SUPPORTS", null, testUser.getId());
        
        // When
        List<Note> linkedNotes = noteService.findLinkedNotes(sourceNote.getId(), testUser.getId());
        
        // Then
        assertThat(linkedNotes).hasSize(2);
        assertThat(linkedNotes).extracting(Note::getId)
                .containsExactlyInAnyOrder(target1.getId(), target2.getId());
    }
    
    @Test
    void shouldFindBacklinks() {
        // Given
        Note targetNote = noteService.createNote("Target Note", "Target Content", null, testUser);
        Note source1 = noteService.createNote("Source 1", "Content 1", null, testUser);
        Note source2 = noteService.createNote("Source 2", "Content 2", null, testUser);
        
        noteService.linkNotes(source1.getId(), targetNote.getId(), "REFERENCES", null, testUser.getId());
        noteService.linkNotes(source2.getId(), targetNote.getId(), "SUPPORTS", null, testUser.getId());
        
        // When
        List<Note> backlinks = noteService.findBacklinks(targetNote.getId(), testUser.getId());
        
        // Then
        assertThat(backlinks).hasSize(2);
        assertThat(backlinks).extracting(Note::getId)
                .containsExactlyInAnyOrder(source1.getId(), source2.getId());
    }
    
    // ============================================================================
    // SEARCH AND DISCOVERY TESTS
    // ============================================================================
    
    @Test
    void shouldSearchNotesByContent() {
        // Given
        noteService.createNote("Java Programming", "Learn about Java streams and lambdas", 
                List.of("java", "programming"), testUser);
        noteService.createNote("Python Basics", "Introduction to Python programming", 
                List.of("python", "programming"), testUser);
        noteService.createNote("Database Design", "SQL and NoSQL databases", 
                List.of("database"), testUser);
        
        // When
        List<Note> results = noteService.searchNotes("programming", testUser.getId());
        
        // Then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(Note::getTitle)
                .containsExactlyInAnyOrder("Java Programming", "Python Basics");
    }
    
    @Test
    void shouldFindNotesByTag() {
        // Given
        noteService.createNote("Note 1", "Content 1", List.of("java", "programming"), testUser);
        noteService.createNote("Note 2", "Content 2", List.of("python", "programming"), testUser);
        noteService.createNote("Note 3", "Content 3", List.of("database"), testUser);
        
        // When
        List<Note> results = noteService.findByTag("programming", testUser.getId());
        
        // Then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(Note::getTitle)
                .containsExactlyInAnyOrder("Note 1", "Note 2");
    }
    
    @Test
    void shouldFindNotesByStatus() {
        // Given
        Note draft = noteService.createNote("Draft Note", "Draft Content", null, testUser);
        Note published = noteService.createNote("Published Note", "Published Content", null, testUser);
        published.updateStatus("PUBLISHED");
        noteRepository.save(published);
        
        // When
        List<Note> draftNotes = noteService.findByStatus("DRAFT", testUser.getId());
        List<Note> publishedNotes = noteService.findByStatus("PUBLISHED", testUser.getId());
        
        // Then
        assertThat(draftNotes).hasSize(1);
        assertThat(draftNotes.get(0).getId()).isEqualTo(draft.getId());
        assertThat(publishedNotes).hasSize(1);
        assertThat(publishedNotes.get(0).getId()).isEqualTo(published.getId());
    }
    
    @Test
    void shouldFindFavoriteNotes() {
        // Given
        Note regularNote = noteService.createNote("Regular Note", "Regular Content", null, testUser);
        Note favoriteNote = noteService.createNote("Favorite Note", "Favorite Content", null, testUser);
        noteService.toggleFavorite(favoriteNote.getId(), testUser.getId());
        
        // When
        List<Note> favorites = noteService.findFavoriteNotes(testUser.getId());
        
        // Then
        assertThat(favorites).hasSize(1);
        assertThat(favorites.get(0).getId()).isEqualTo(favoriteNote.getId());
        assertThat(favorites.get(0).isFavorite()).isTrue();
    }
    
    // ============================================================================
    // UTILITY OPERATION TESTS
    // ============================================================================
    
    @Test
    void shouldToggleFavoriteStatus() {
        // Given
        Note note = noteService.createNote("Test Note", "Test Content", null, testUser);
        assertThat(note.isFavorite()).isFalse();
        
        // When - toggle to favorite
        Note favoriteNote = noteService.toggleFavorite(note.getId(), testUser.getId());
        
        // Then
        assertThat(favoriteNote.isFavorite()).isTrue();
        
        // When - toggle back to not favorite
        Note regularNote = noteService.toggleFavorite(note.getId(), testUser.getId());
        
        // Then
        assertThat(regularNote.isFavorite()).isFalse();
    }
    
    @Test
    void shouldUpdateNoteStatus() {
        // Given
        Note note = noteService.createNote("Test Note", "Test Content", null, testUser);
        assertThat(note.getStatus()).isEqualTo("DRAFT");
        
        // When
        Note publishedNote = noteService.updateStatus(note.getId(), "PUBLISHED", testUser.getId());
        
        // Then
        assertThat(publishedNote.getStatus()).isEqualTo("PUBLISHED");
        assertThat(publishedNote.getUpdatedAt()).isAfter(note.getUpdatedAt());
    }
    
    // ============================================================================
    // VALIDATION TESTS
    // ============================================================================
    
    @Test
    void shouldValidateNoteTitle() {
        // When & Then - empty title
        assertThatThrownBy(() -> noteService.createNote("", "Content", null, testUser))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Note title is required");
        
        // When & Then - null title
        assertThatThrownBy(() -> noteService.createNote(null, "Content", null, testUser))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Note title is required");
        
        // When & Then - title too long
        String longTitle = "a".repeat(201);
        assertThatThrownBy(() -> noteService.createNote(longTitle, "Content", null, testUser))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Title cannot exceed 200 characters");
    }
    
    @Test
    void shouldValidateSearchQuery() {
        // When & Then - empty query
        assertThatThrownBy(() -> noteService.searchNotes("", testUser.getId()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Search query cannot be empty");
        
        // When & Then - query too short
        assertThatThrownBy(() -> noteService.searchNotes("a", testUser.getId()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Search query must be at least 2 characters long");
    }
    
    @Test
    void shouldValidateLinkType() {
        // Given
        Note sourceNote = noteService.createNote("Source", "Content", null, testUser);
        Note targetNote = noteService.createNote("Target", "Content", null, testUser);
        
        // When & Then - invalid link type
        assertThatThrownBy(() -> noteService.linkNotes(
                sourceNote.getId(), targetNote.getId(), "INVALID_TYPE", null, testUser.getId()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid link type");
    }
}