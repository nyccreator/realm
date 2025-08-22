package com.realm;

import com.realm.model.User;
import com.realm.model.Note;
import com.realm.model.NoteLink;
import com.realm.repository.UserRepository;
import com.realm.repository.NoteRepository;
import com.realm.service.AuthService;
import com.realm.service.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Neo4j schema and authentication system.
 * 
 * This test validates the complete Neo4j schema implementation for Section 3.1,
 * including User entity operations, authentication flows, JWT token handling,
 * and basic graph relationships between User and Note entities.
 * 
 * Test Coverage:
 * - Neo4j connection and schema validation
 * - User entity CRUD operations
 * - Authentication service functionality
 * - JWT token generation and validation
 * - Basic graph relationships
 * - Repository query optimization
 */
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class Neo4jSchemaIntegrationTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private NoteRepository noteRepository;
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Test
    public void testNeo4jConnection() {
        assertNotNull(userRepository, "UserRepository should be injected");
        assertNotNull(noteRepository, "NoteRepository should be injected");
        assertNotNull(authService, "AuthService should be injected");
        assertNotNull(jwtTokenProvider, "JwtTokenProvider should be injected");
    }
    
    @Test
    public void testUserEntityOperations() {
        // Create a test user
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("hashedPassword123")
                .displayName("Test User")
                .firstName("Test")
                .lastName("User")
                .bio("Test user for schema validation")
                .isActive(true)
                .isVerified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        // Test user creation
        User savedUser = userRepository.save(user);
        assertNotNull(savedUser.getId(), "User ID should be generated");
        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals("Test User", savedUser.getDisplayName());
        assertTrue(savedUser.isActive());
        assertTrue(savedUser.isVerified());
        
        // Test user retrieval by email
        Optional<User> foundUser = userRepository.findByEmail("test@example.com");
        assertTrue(foundUser.isPresent(), "User should be found by email");
        assertEquals(savedUser.getId(), foundUser.get().getId());
        
        // Test active user lookup
        Optional<User> activeUser = userRepository.findActiveUserByEmail("test@example.com");
        assertTrue(activeUser.isPresent(), "Active user should be found");
        
        // Test verified user lookup
        Optional<User> verifiedUser = userRepository.findVerifiedUserByEmail("test@example.com");
        assertTrue(verifiedUser.isPresent(), "Verified user should be found");
        
        // Test email existence check
        assertTrue(userRepository.existsByEmail("test@example.com"), "Email should exist");
        assertFalse(userRepository.existsByEmail("nonexistent@example.com"), "Non-existent email should return false");
        
        // Test user profile update
        savedUser.updateProfile("Updated Name", "Updated", "User", "Updated bio");
        User updatedUser = userRepository.save(savedUser);
        assertEquals("Updated Name", updatedUser.getDisplayName());
        assertEquals("Updated bio", updatedUser.getBio());
        
        // Test preference management
        savedUser.updatePreference("theme", "dark");
        savedUser.updatePreference("language", "en");
        User userWithPrefs = userRepository.save(savedUser);
        assertEquals("dark", userWithPrefs.getPreference("theme"));
        assertEquals("en", userWithPrefs.getPreference("language"));
        
        // Cleanup
        userRepository.delete(savedUser);
    }
    
    @Test
    public void testUserNoteRelationships() {
        // Create test user
        User user = User.builder()
                .email("noteuser@example.com")
                .passwordHash("hashedPassword123")
                .displayName("Note User")
                .isActive(true)
                .isVerified(true)
                .build();
        User savedUser = userRepository.save(user);
        
        // Create test note
        Note note = Note.builder()
                .title("Test Note")
                .content("This is a test note content")
                .summary("Test note summary")
                .status("DRAFT")
                .priority("NORMAL")
                .isFavorite(false)
                .createdBy(savedUser)
                .build();
        Note savedNote = noteRepository.save(note);
        
        // Verify note-user relationship
        assertNotNull(savedNote.getId(), "Note ID should be generated");
        assertEquals("Test Note", savedNote.getTitle());
        assertEquals(savedUser.getId(), savedNote.getCreatedBy().getId());
        
        // Test note updates
        savedNote.updateContent("Updated Note Title", "Updated content");
        savedNote.addTag("test");
        savedNote.addTag("schema");
        savedNote.toggleFavorite();
        Note updatedNote = noteRepository.save(savedNote);
        
        assertEquals("Updated Note Title", updatedNote.getTitle());
        assertTrue(updatedNote.isFavorite());
        assertTrue(updatedNote.getTags().contains("test"));
        assertTrue(updatedNote.getTags().contains("schema"));
        
        // Cleanup
        noteRepository.delete(savedNote);
        userRepository.delete(savedUser);
    }
    
    @Test
    public void testAuthenticationService() {
        // Test user registration
        AuthService.AuthResult registerResult = authService.registerUser(
                "authtest@example.com",
                "StrongPassword123!",
                "Auth Test User",
                "Auth",
                "User"
        );
        
        assertTrue(registerResult.isSuccess(), "User registration should succeed");
        assertNotNull(registerResult.getAccessToken(), "Access token should be generated");
        assertNotNull(registerResult.getRefreshToken(), "Refresh token should be generated");
        assertNotNull(registerResult.getUser(), "User should be returned");
        assertEquals("authtest@example.com", registerResult.getUser().getEmail());
        
        // Test user authentication
        AuthService.AuthResult loginResult = authService.authenticateUser(
                "authtest@example.com",
                "StrongPassword123!"
        );
        
        assertTrue(loginResult.isSuccess(), "User authentication should succeed");
        assertNotNull(loginResult.getAccessToken(), "Access token should be generated");
        assertNotNull(loginResult.getRefreshToken(), "Refresh token should be generated");
        
        // Test token refresh
        AuthService.AuthResult refreshResult = authService.refreshToken(loginResult.getRefreshToken());
        assertTrue(refreshResult.isSuccess(), "Token refresh should succeed");
        assertNotNull(refreshResult.getAccessToken(), "New access token should be generated");
        
        // Test profile update
        AuthService.AuthResult profileResult = authService.updateProfile(
                registerResult.getUser().getId(),
                "Updated Auth User",
                "Updated",
                "User",
                "Updated bio"
        );
        
        assertTrue(profileResult.isSuccess(), "Profile update should succeed");
        assertEquals("Updated Auth User", profileResult.getUser().getDisplayName());
        
        // Cleanup
        userRepository.delete(registerResult.getUser());
    }
    
    @Test
    public void testJwtTokenOperations() {
        // Test token generation
        String token = jwtTokenProvider.generateTokenFromUsername("testuser@example.com");
        assertNotNull(token, "Token should be generated");
        assertTrue(token.contains("."), "Token should be in JWT format");
        
        // Test token validation
        assertTrue(jwtTokenProvider.validateToken(token), "Token should be valid");
        
        // Test username extraction
        String username = jwtTokenProvider.getUsernameFromToken(token);
        assertEquals("testuser@example.com", username, "Username should be extracted correctly");
        
        // Test token expiration check
        assertFalse(jwtTokenProvider.isTokenExpired(token), "Fresh token should not be expired");
        
        // Test refresh token generation
        String refreshToken = jwtTokenProvider.generateRefreshToken("testuser@example.com");
        assertNotNull(refreshToken, "Refresh token should be generated");
        assertTrue(jwtTokenProvider.isRefreshToken(refreshToken), "Should be identified as refresh token");
        assertFalse(jwtTokenProvider.isRefreshToken(token), "Access token should not be identified as refresh token");
        
        // Test remaining time calculation
        long remainingTime = jwtTokenProvider.getRemainingTimeToExpire(token);
        assertTrue(remainingTime > 0, "Fresh token should have remaining time");
    }
    
    @Test
    public void testRepositoryQueryPerformance() {
        // Create multiple test users for performance testing
        for (int i = 1; i <= 10; i++) {
            User user = User.builder()
                    .email("perftest" + i + "@example.com")
                    .passwordHash("password" + i)
                    .displayName("Performance Test User " + i)
                    .isActive(i % 2 == 1) // Half active, half inactive
                    .isVerified(i % 3 == 1) // Some verified, some not
                    .build();
            userRepository.save(user);
        }
        
        // Test query performance (basic timing check)
        long startTime = System.currentTimeMillis();
        
        // Test various repository queries
        userRepository.findByEmail("perftest5@example.com");
        userRepository.findActiveUserByEmail("perftest5@example.com");
        userRepository.findVerifiedUserByEmail("perftest3@example.com");
        userRepository.existsByEmail("perftest7@example.com");
        
        long endTime = System.currentTimeMillis();
        long queryTime = endTime - startTime;
        
        // Queries should complete within reasonable time (adjust based on requirements)
        assertTrue(queryTime < 1000, "Repository queries should complete within 1 second");
        
        // Cleanup performance test users
        for (int i = 1; i <= 10; i++) {
            userRepository.findByEmail("perftest" + i + "@example.com")
                    .ifPresent(userRepository::delete);
        }
    }
    
    @Test
    public void testGraphRelationshipIntegrity() {
        // Create user and multiple notes to test relationship integrity
        User user = User.builder()
                .email("graphtest@example.com")
                .passwordHash("password123")
                .displayName("Graph Test User")
                .isActive(true)
                .isVerified(true)
                .build();
        User savedUser = userRepository.save(user);
        
        // Create multiple notes
        Note note1 = Note.builder()
                .title("Note 1")
                .content("Content 1")
                .createdBy(savedUser)
                .build();
        
        Note note2 = Note.builder()
                .title("Note 2")
                .content("Content 2")
                .createdBy(savedUser)
                .build();
        
        Note savedNote1 = noteRepository.save(note1);
        Note savedNote2 = noteRepository.save(note2);
        
        // Create relationship between notes
        NoteLink link = NoteLink.builder()
                .targetNote(savedNote2)
                .type(NoteLink.TYPE_REFERENCES)
                .context("Test relationship")
                .strength(0.8)
                .build();
        
        savedNote1.getOutgoingLinks().add(link);
        noteRepository.save(savedNote1);
        
        // Verify relationship integrity
        Optional<Note> retrievedNote1 = noteRepository.findById(savedNote1.getId());
        assertTrue(retrievedNote1.isPresent(), "Note should be retrievable");
        assertFalse(retrievedNote1.get().getOutgoingLinks().isEmpty(), "Note should have outgoing links");
        
        NoteLink retrievedLink = retrievedNote1.get().getOutgoingLinks().iterator().next();
        assertEquals(NoteLink.TYPE_REFERENCES, retrievedLink.getType());
        assertEquals("Test relationship", retrievedLink.getContext());
        assertEquals(0.8, retrievedLink.getStrength());
        assertEquals(savedNote2.getId(), retrievedLink.getTargetNote().getId());
        
        // Cleanup
        noteRepository.delete(savedNote1);
        noteRepository.delete(savedNote2);
        userRepository.delete(savedUser);
    }
}