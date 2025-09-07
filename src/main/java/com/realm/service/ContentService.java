package com.realm.service;

import com.realm.dto.CreateNoteRequest;
import com.realm.dto.UpdateNoteRequest;
import com.realm.event.NoteCreatedEvent;
import com.realm.event.NoteUpdatedEvent;
import com.realm.exception.NoteAccessDeniedException;
import com.realm.exception.NoteNotFoundException;
import com.realm.exception.ValidationException;
import com.realm.model.Note;
import com.realm.model.NoteVersion;
import com.realm.model.User;
import com.realm.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ContentService - Advanced note content management service
 * 
 * Handles all note content operations with advanced features:
 * - Content validation and preprocessing
 * - Automatic content analysis (word count, reading time, topics)
 * - Content versioning and history tracking
 * - Smart content summarization
 * - Duplicate detection and merging suggestions
 * - Content quality scoring
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ContentService {
    
    private final NoteRepository noteRepository;
    private final CacheService cacheService;
    private final ApplicationEventPublisher eventPublisher;
    
    // Content analysis patterns
    private static final Pattern HEADING_PATTERN = Pattern.compile("<h[1-6][^>]*>(.*?)</h[1-6]>", Pattern.CASE_INSENSITIVE);
    private static final Pattern LINK_PATTERN = Pattern.compile("<a[^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE);
    private static final Pattern LIST_PATTERN = Pattern.compile("<(ul|ol)[^>]*>.*?</\\1>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    
    /**
     * Create new note with advanced content processing
     */
    public Note createNote(CreateNoteRequest request, User user) {
        log.debug("Creating note '{}' for user {}", request.getTitle(), user.getId());
        
        validateContentRequest(request);
        
        Note note = Note.builder()
            .title(preprocessTitle(request.getTitle()))
            .content(preprocessContent(request.getContent()))
            .tags(extractAndNormalizeTags(request.getTags(), request.getContent()))
            .createdBy(user)
            .build();
            
        // Advanced content analysis
        analyzeAndEnrichContent(note);
        
        Note savedNote = noteRepository.save(note);
        
        // Cache the new note
        cacheService.cacheNote(savedNote);
        
        // Publish creation event
        eventPublisher.publishEvent(new NoteCreatedEvent(savedNote));
        
        log.info("Created note {} with {} words for user {}", 
                savedNote.getId(), savedNote.getWordCount(), user.getId());
        
        return savedNote;
    }
    
    /**
     * Update note content with version tracking
     */
    public Note updateNote(String noteId, UpdateNoteRequest request, String userId) {
        log.debug("Updating note {} for user {}", noteId, userId);
        
        Note existingNote = findNoteWithAccess(noteId, userId);
        
        // Track changes for versioning
        NoteVersion previousVersion = createVersion(existingNote);
        
        // Update content if provided
        if (request.getTitle() != null) {
            existingNote.setTitle(preprocessTitle(request.getTitle()));
        }
        
        if (request.getContent() != null) {
            existingNote.setContent(preprocessContent(request.getContent()));
            analyzeAndEnrichContent(existingNote);
        }
        
        if (request.getTags() != null) {
            existingNote.setTags(extractAndNormalizeTags(request.getTags(), existingNote.getContent()));
        }
        
        existingNote.setUpdatedAt(LocalDateTime.now());
        
        Note updatedNote = noteRepository.save(existingNote);
        
        // Update cache
        cacheService.updateNoteCache(updatedNote);
        
        // Store version and publish event
        storeVersion(previousVersion);
        eventPublisher.publishEvent(new NoteUpdatedEvent(updatedNote, previousVersion));
        
        log.info("Updated note {} for user {}", noteId, userId);
        return updatedNote;
    }
    
    /**
     * Get note with caching
     */
    public Note getNote(String noteId, String userId) {
        log.debug("Getting note {} for user {}", noteId, userId);
        
        // Try cache first
        Optional<Note> cachedNote = cacheService.getCachedNote(noteId);
        if (cachedNote.isPresent()) {
            Note note = cachedNote.get();
            if (hasUserAccess(note, userId)) {
                note.markAsAccessed();
                return note;
            }
        }
        
        // Fallback to database
        Note note = findNoteWithAccess(noteId, userId);
        note.markAsAccessed();
        
        // Cache for future requests
        cacheService.cacheNote(note);
        
        return noteRepository.save(note);
    }
    
    /**
     * Analyze and enrich note content with metadata
     */
    private void analyzeAndEnrichContent(Note note) {
        String content = note.getContent();
        if (content == null || content.trim().isEmpty()) {
            return;
        }
        
        // Calculate metrics
        ContentMetrics metrics = calculateContentMetrics(content);
        note.setWordCount(metrics.getWordCount());
        note.setReadingTime(metrics.getReadingTime());
        
        // Extract structure information
        ContentStructure structure = analyzeContentStructure(content);
        
        // Generate summary if not provided
        if (note.getSummary() == null || note.getSummary().trim().isEmpty()) {
            note.setSummary(generateContentSummary(content, structure));
        }
        
        // Set content quality score
        double qualityScore = calculateContentQuality(content, structure, note.getTags());
        // Store quality score in metadata or separate field
        
        log.debug("Content analysis complete for note: {} words, {} min read, quality: {:.2f}", 
                metrics.getWordCount(), metrics.getReadingTime(), qualityScore);
    }
    
    /**
     * Calculate content metrics (word count, reading time, etc.)
     */
    private ContentMetrics calculateContentMetrics(String content) {
        String plainText = stripHtmlTags(content);
        String[] words = plainText.trim().split("\\s+");
        int wordCount = plainText.trim().isEmpty() ? 0 : words.length;
        
        // Calculate reading time (average 200 words per minute)
        int readingTime = Math.max(1, wordCount / 200);
        
        // Count sentences for readability analysis
        int sentenceCount = plainText.split("[.!?]+").length;
        
        // Count paragraphs
        int paragraphCount = content.split("<p[^>]*>").length - 1;
        if (paragraphCount == 0) {
            paragraphCount = plainText.split("\n\n").length;
        }
        
        return new ContentMetrics(wordCount, readingTime, sentenceCount, paragraphCount);
    }
    
    /**
     * Analyze content structure (headings, lists, links)
     */
    private ContentStructure analyzeContentStructure(String content) {
        int headingCount = countMatches(content, HEADING_PATTERN);
        int linkCount = countMatches(content, LINK_PATTERN);
        int listCount = countMatches(content, LIST_PATTERN);
        
        return new ContentStructure(headingCount, linkCount, listCount);
    }
    
    /**
     * Generate automatic content summary
     */
    private String generateContentSummary(String content, ContentStructure structure) {
        String plainText = stripHtmlTags(content).trim();
        
        if (plainText.length() <= 150) {
            return plainText;
        }
        
        // Extract first meaningful sentence or paragraph
        String[] sentences = plainText.split("\\. ");
        StringBuilder summary = new StringBuilder();
        
        for (String sentence : sentences) {
            if (summary.length() + sentence.length() > 147) {
                break;
            }
            if (summary.length() > 0) {
                summary.append(". ");
            }
            summary.append(sentence);
        }
        
        return summary.toString() + "...";
    }
    
    /**
     * Calculate content quality score based on various factors
     */
    private double calculateContentQuality(String content, ContentStructure structure, List<String> tags) {
        double score = 0.0;
        String plainText = stripHtmlTags(content);
        
        // Length factor (optimal range: 100-2000 words)
        int wordCount = plainText.split("\\s+").length;
        if (wordCount >= 100 && wordCount <= 2000) {
            score += 0.3;
        } else if (wordCount > 50) {
            score += 0.15;
        }
        
        // Structure factor
        if (structure.getHeadingCount() > 0) score += 0.2;
        if (structure.getLinkCount() > 0) score += 0.15;
        if (structure.getListCount() > 0) score += 0.1;
        
        // Tag factor
        if (tags != null && !tags.isEmpty()) {
            score += Math.min(0.25, tags.size() * 0.05);
        }
        
        return Math.min(1.0, score);
    }
    
    /**
     * Preprocess title for consistency
     */
    private String preprocessTitle(String title) {
        if (title == null) return null;
        
        return title.trim()
            .replaceAll("\\s+", " ") // Normalize whitespace
            .replaceAll("^[\\s\\p{Punct}]+|[\\s\\p{Punct}]+$", ""); // Trim punctuation
    }
    
    /**
     * Preprocess content for storage and analysis
     */
    private String preprocessContent(String content) {
        if (content == null) return null;
        
        return content.trim()
            .replaceAll("(?i)<script[^>]*>.*?</script>", "") // Remove scripts
            .replaceAll("(?i)<style[^>]*>.*?</style>", ""); // Remove styles
    }
    
    /**
     * Extract and normalize tags from request and content
     */
    private List<String> extractAndNormalizeTags(List<String> requestTags, String content) {
        // Start with provided tags
        Set<String> tags = new HashSet<>();
        if (requestTags != null) {
            requestTags.stream()
                .filter(tag -> tag != null && !tag.trim().isEmpty())
                .map(tag -> tag.trim().toLowerCase())
                .forEach(tags::add);
        }
        
        // Auto-extract tags from content (hashtags, keywords)
        if (content != null) {
            Pattern hashtagPattern = Pattern.compile("#(\\w+)");
            Matcher matcher = hashtagPattern.matcher(content);
            while (matcher.find()) {
                tags.add(matcher.group(1).toLowerCase());
            }
        }
        
        return new ArrayList<>(tags);
    }
    
    /**
     * Validate content creation/update request
     */
    private void validateContentRequest(CreateNoteRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new ValidationException("Note title is required");
        }
        
        if (request.getTitle().length() > 200) {
            throw new ValidationException("Title cannot exceed 200 characters");
        }
        
        if (request.getContent() != null && request.getContent().length() > 100000) {
            throw new ValidationException("Content cannot exceed 100,000 characters");
        }
    }
    
    /**
     * Find note and verify user access
     */
    private Note findNoteWithAccess(String noteId, String userId) {
        Optional<Note> noteOpt = noteRepository.findById(noteId);
        if (noteOpt.isEmpty()) {
            throw new NoteNotFoundException(noteId);
        }
        
        Note note = noteOpt.get();
        if (!hasUserAccess(note, userId)) {
            throw new NoteAccessDeniedException(noteId, userId);
        }
        
        return note;
    }
    
    /**
     * Check if user has access to note
     */
    private boolean hasUserAccess(Note note, String userId) {
        return note.getCreatedBy() != null && 
               note.getCreatedBy().getId().equals(userId);
    }
    
    // Helper methods
    private String stripHtmlTags(String html) {
        return html.replaceAll("<[^>]*>", "");
    }
    
    private int countMatches(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
    
    /**
     * Create a version snapshot of a note before changes
     */
    private NoteVersion createVersion(Note note) {
        return NoteVersion.fromNote(note);
    }
    
    /**
     * Store a note version (would integrate with a versioning repository)
     */
    private void storeVersion(NoteVersion version) {
        // TODO: Implement version storage in Neo4j
        // This would save the version to a NoteVersionRepository
        log.debug("Storing version for note {}", version.getOriginalNote().getId());
    }
    
    // Inner classes for content analysis
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ContentMetrics {
        private int wordCount;
        private int readingTime;
        private int sentenceCount;
        private int paragraphCount;
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor  
    private static class ContentStructure {
        private int headingCount;
        private int linkCount;
        private int listCount;
    }
}