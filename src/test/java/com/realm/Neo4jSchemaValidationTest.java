package com.realm;

import com.realm.model.Note;
import com.realm.model.NoteLink;
import com.realm.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.security.core.userdetails.UserDetails;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Schema validation tests for Neo4j entities.
 * These tests validate the graph schema structure without requiring a running Neo4j instance.
 */
class Neo4jSchemaValidationTest {

    @Test
    void userEntity_shouldHaveCorrectNodeAnnotation() {
        Node nodeAnnotation = User.class.getAnnotation(Node.class);
        assertNotNull(nodeAnnotation, "User class should have @Node annotation");
        assertEquals("User", nodeAnnotation.value()[0], "User node should have correct label");
    }

    @Test
    void userEntity_shouldImplementUserDetails() {
        assertTrue(UserDetails.class.isAssignableFrom(User.class), 
                  "User should implement UserDetails for Spring Security integration");
    }

    @Test
    void userEntity_shouldHaveRequiredFields() throws NoSuchFieldException {
        // Verify essential authentication fields
        assertNotNull(User.class.getDeclaredField("id"), "User should have id field");
        assertNotNull(User.class.getDeclaredField("email"), "User should have email field");
        assertNotNull(User.class.getDeclaredField("passwordHash"), "User should have passwordHash field");
        assertNotNull(User.class.getDeclaredField("displayName"), "User should have displayName field");
        assertNotNull(User.class.getDeclaredField("createdAt"), "User should have createdAt field");
        assertNotNull(User.class.getDeclaredField("updatedAt"), "User should have updatedAt field");
    }

    @Test
    void noteEntity_shouldHaveCorrectNodeAnnotation() {
        Node nodeAnnotation = Note.class.getAnnotation(Node.class);
        assertNotNull(nodeAnnotation, "Note class should have @Node annotation");
        assertEquals("Note", nodeAnnotation.value()[0], "Note node should have correct label");
    }

    @Test
    void noteEntity_shouldHaveRequiredFields() throws NoSuchFieldException {
        // Verify essential PKM fields
        assertNotNull(Note.class.getDeclaredField("id"), "Note should have id field");
        assertNotNull(Note.class.getDeclaredField("title"), "Note should have title field");
        assertNotNull(Note.class.getDeclaredField("content"), "Note should have content field");
        assertNotNull(Note.class.getDeclaredField("tags"), "Note should have tags field");
        assertNotNull(Note.class.getDeclaredField("createdAt"), "Note should have createdAt field");
        assertNotNull(Note.class.getDeclaredField("updatedAt"), "Note should have updatedAt field");
    }

    @Test
    void noteEntity_shouldHaveGraphRelationships() throws NoSuchFieldException {
        Field createdByField = Note.class.getDeclaredField("createdBy");
        Relationship relationshipAnnotation = createdByField.getAnnotation(Relationship.class);
        
        assertNotNull(relationshipAnnotation, "Note should have relationship to User");
        assertEquals("CREATED_BY", relationshipAnnotation.type(), "Should have correct relationship type");
        assertEquals(Relationship.Direction.OUTGOING, relationshipAnnotation.direction(), 
                    "Should have correct relationship direction");
    }

    @Test
    void noteLinkEntity_shouldHaveCorrectRelationshipPropertiesAnnotation() {
        RelationshipProperties annotation = NoteLink.class.getAnnotation(RelationshipProperties.class);
        assertNotNull(annotation, "NoteLink class should have @RelationshipProperties annotation");
    }

    @Test
    void noteLinkEntity_shouldHaveRequiredFields() throws NoSuchFieldException {
        // Verify relationship metadata fields
        assertNotNull(NoteLink.class.getDeclaredField("id"), "NoteLink should have id field");
        assertNotNull(NoteLink.class.getDeclaredField("type"), "NoteLink should have type field");
        assertNotNull(NoteLink.class.getDeclaredField("strength"), "NoteLink should have strength field");
        assertNotNull(NoteLink.class.getDeclaredField("context"), "NoteLink should have context field");
        assertNotNull(NoteLink.class.getDeclaredField("targetNote"), "NoteLink should have targetNote field");
        assertNotNull(NoteLink.class.getDeclaredField("createdAt"), "NoteLink should have createdAt field");
    }

    @Test
    void userEntity_canBeInstantiated() {
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("hashed_password")
                .displayName("Test User")
                .build();

        assertNotNull(user, "User should be instantiable");
        assertEquals("test@example.com", user.getEmail());
        assertEquals("Test User", user.getDisplayName());
        assertNotNull(user.getCreatedAt());
        assertTrue(user.isActive());
    }

    @Test
    void noteEntity_canBeInstantiated() {
        User user = User.builder()
                .email("test@example.com")
                .displayName("Test User")
                .build();

        Note note = Note.builder()
                .title("Test Note")
                .content("This is test content")
                .createdBy(user)
                .build();

        assertNotNull(note, "Note should be instantiable");
        assertEquals("Test Note", note.getTitle());
        assertEquals("This is test content", note.getContent());
        assertNotNull(note.getCreatedAt());
        assertEquals(user, note.getCreatedBy());
    }

    @Test
    void noteLinkEntity_canBeInstantiated() {
        User user = User.builder()
                .email("test@example.com")
                .displayName("Test User")
                .build();

        Note sourceNote = Note.builder()
                .title("Source Note")
                .content("Source content")
                .createdBy(user)
                .build();

        Note targetNote = Note.builder()
                .title("Target Note")
                .content("Target content")
                .createdBy(user)
                .build();

        NoteLink link = NoteLink.builder()
                .type(NoteLink.TYPE_REFERENCES)
                .context("Test relationship")
                .targetNote(targetNote)
                .build();

        assertNotNull(link, "NoteLink should be instantiable");
        assertEquals(NoteLink.TYPE_REFERENCES, link.getType());
        assertEquals("Test relationship", link.getContext());
        assertEquals(targetNote, link.getTargetNote());
        assertNotNull(link.getCreatedAt());
        assertEquals(1.0, link.getStrength());
    }

    @Test
    void noteLinkConstants_shouldBeCorrect() {
        // Verify relationship type constants
        assertEquals("REFERENCES", NoteLink.TYPE_REFERENCES);
        assertEquals("SUPPORTS", NoteLink.TYPE_SUPPORTS);
        assertEquals("CONTRADICTS", NoteLink.TYPE_CONTRADICTS);
        assertEquals("BUILDS_ON", NoteLink.TYPE_BUILDS_ON);
        assertEquals("RELATED_TO", NoteLink.TYPE_RELATED_TO);
    }

    @Test
    void userMethods_shouldWorkCorrectly() {
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("hashed_password")
                .displayName("Test User")
                .firstName("Test")
                .lastName("User")
                .build();

        // Test UserDetails methods
        assertEquals("test@example.com", user.getUsername());
        assertEquals("hashed_password", user.getPassword());
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
        assertFalse(user.isEnabled()); // Should be false until verified

        // Test utility methods
        assertEquals("Test User", user.getFullName());
        user.updateLastLogin();
        assertNotNull(user.getLastLoginAt());
    }

    @Test
    void noteUtilityMethods_shouldWorkCorrectly() {
        User user = User.builder()
                .email("test@example.com")
                .displayName("Test User")
                .build();

        Note note = Note.builder()
                .title("Test Note")
                .content("This is a test note with some content for searching")
                .createdBy(user)
                .build();

        // Test search functionality
        assertTrue(note.containsSearchTerm("test"));
        assertTrue(note.containsSearchTerm("content"));
        assertFalse(note.containsSearchTerm("nonexistent"));

        // Test tag management
        note.addTag("important");
        note.addTag("test");
        assertTrue(note.getTags().contains("important"));
        assertTrue(note.getTags().contains("test"));

        note.removeTag("test");
        assertFalse(note.getTags().contains("test"));

        // Test status updates
        note.updateStatus("PUBLISHED");
        assertEquals("PUBLISHED", note.getStatus());

        // Test favorite toggle
        assertFalse(note.isFavorite());
        note.toggleFavorite();
        assertTrue(note.isFavorite());
    }

    @Test
    void noteLinkUtilityMethods_shouldWorkCorrectly() {
        User user = User.builder()
                .email("test@example.com")
                .displayName("Test User")
                .build();

        Note targetNote = Note.builder()
                .title("Target Note")
                .content("Target content")
                .createdBy(user)
                .build();

        NoteLink link = NoteLink.builder()
                .type(NoteLink.TYPE_REFERENCES)
                .strength(0.8)
                .targetNote(targetNote)
                .build();

        // Test strength classification
        assertTrue(link.isStrongLink()); // 0.8 >= 0.7
        assertFalse(link.isWeakLink()); // 0.8 >= 0.3

        link.updateStrength(0.2);
        assertTrue(link.isWeakLink()); // 0.2 < 0.3
        assertFalse(link.isStrongLink()); // 0.2 < 0.7

        // Test type validation
        assertTrue(NoteLink.isValidType(NoteLink.TYPE_SUPPORTS));
        assertFalse(NoteLink.isValidType("INVALID_TYPE"));

        // Test display methods
        assertEquals("References", link.getDisplayType());
        link.updateType(NoteLink.TYPE_SUPPORTS);
        assertEquals("Supports", link.getDisplayType());
    }
}