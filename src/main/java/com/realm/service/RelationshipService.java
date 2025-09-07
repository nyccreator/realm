package com.realm.service;

import com.realm.dto.LinkNotesRequest;
import com.realm.event.RelationshipCreatedEvent;
import com.realm.event.RelationshipRemovedEvent;
import com.realm.exception.NoteAccessDeniedException;
import com.realm.exception.NoteNotFoundException;
import com.realm.exception.ValidationException;
import com.realm.model.Note;
import com.realm.model.NoteLink;
import com.realm.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RelationshipService - Advanced graph relationship management service
 * 
 * Handles sophisticated note relationship operations:
 * - Semantic relationship creation and management
 * - Graph traversal and pathfinding algorithms
 * - Relationship strength calculation
 * - Automatic relationship suggestions
 * - Circular dependency detection
 * - Graph clustering and community detection
 * - Relationship analytics and insights
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RelationshipService {
    
    private final NoteRepository noteRepository;
    private final CacheService cacheService;
    private final ApplicationEventPublisher eventPublisher;
    
    // Relationship types with semantic meaning
    public static final String REFERENCES = "references";
    public static final String CONTRADICTS = "contradicts";
    public static final String SUPPORTS = "supports";
    public static final String EXPLAINS = "explains";
    public static final String EXEMPLIFIES = "exemplifies";
    public static final String GENERALIZES = "generalizes";
    public static final String SPECIALIZES = "specializes";
    public static final String FOLLOWS_FROM = "follows_from";
    public static final String PREREQUISITE = "prerequisite";
    public static final String RELATED_TO = "related_to";
    
    private static final Set<String> VALID_RELATIONSHIP_TYPES = Set.of(
        REFERENCES, CONTRADICTS, SUPPORTS, EXPLAINS, EXEMPLIFIES,
        GENERALIZES, SPECIALIZES, FOLLOWS_FROM, PREREQUISITE, RELATED_TO
    );
    
    /**
     * Create a semantic relationship between notes
     */
    public NoteLink createRelationship(String sourceNoteId, LinkNotesRequest request, String userId) {
        log.debug("Creating relationship {} -> {} of type '{}' for user {}", 
                 sourceNoteId, request.getTargetNoteId(), request.getType(), userId);
        
        // Validate access to both notes
        Note sourceNote = validateNoteAccess(sourceNoteId, userId);
        Note targetNote = validateNoteAccess(request.getTargetNoteId(), userId);
        
        // Prevent self-referencing relationships
        if (sourceNoteId.equals(request.getTargetNoteId())) {
            throw new ValidationException("Cannot create relationship from note to itself");
        }
        
        // Validate relationship type
        String relationshipType = normalizeRelationshipType(request.getType());
        validateRelationshipType(relationshipType);
        
        // Check for existing relationship
        if (hasExistingRelationship(sourceNote, request.getTargetNoteId(), relationshipType)) {
            throw new ValidationException("Relationship already exists between these notes");
        }
        
        // Detect and prevent circular dependencies for certain relationship types
        if (wouldCreateCircularDependency(sourceNote, targetNote, relationshipType)) {
            throw new ValidationException("This relationship would create a circular dependency");
        }
        
        // Create the relationship with enhanced metadata
        NoteLink relationship = createEnhancedRelationship(sourceNote, targetNote, request, relationshipType);
        
        // Add to source note's outgoing relationships
        sourceNote.getOutgoingLinks().add(relationship);
        sourceNote.setUpdatedAt(LocalDateTime.now());
        
        Note updatedSourceNote = noteRepository.save(sourceNote);
        
        // Update caches
        cacheService.updateNoteCache(updatedSourceNote);
        cacheService.invalidateNote(request.getTargetNoteId());
        
        // Publish relationship creation event
        eventPublisher.publishEvent(new RelationshipCreatedEvent(relationship));
        
        log.info("Created relationship {} -> {} of type '{}' for user {}", 
                sourceNoteId, request.getTargetNoteId(), relationshipType, userId);
        
        return relationship;
    }
    
    /**
     * Remove a relationship between notes
     */
    public void removeRelationship(String sourceNoteId, String relationshipId, String userId) {
        log.debug("Removing relationship {} from note {} for user {}", 
                 relationshipId, sourceNoteId, userId);
        
        Note sourceNote = validateNoteAccess(sourceNoteId, userId);
        
        Optional<NoteLink> relationshipOpt = sourceNote.getOutgoingLinks().stream()
            .filter(link -> link.getId() != null && link.getId().equals(relationshipId))
            .findFirst();
        
        if (relationshipOpt.isEmpty()) {
            throw new ValidationException("Relationship not found: " + relationshipId);
        }
        
        NoteLink relationship = relationshipOpt.get();
        sourceNote.getOutgoingLinks().remove(relationship);
        sourceNote.setUpdatedAt(LocalDateTime.now());
        
        noteRepository.save(sourceNote);
        
        // Update caches
        cacheService.updateNoteCache(sourceNote);
        if (relationship.getTargetNote() != null) {
            cacheService.invalidateNote(relationship.getTargetNote().getId());
        }
        
        // Publish relationship removal event
        eventPublisher.publishEvent(new RelationshipRemovedEvent(relationship));
        
        log.info("Removed relationship {} from note {} for user {}", 
                relationshipId, sourceNoteId, userId);
    }
    
    /**
     * Find the shortest path between two notes
     */
    public List<Note> findShortestPath(String startNoteId, String endNoteId, String userId, int maxDepth) {
        log.debug("Finding shortest path from note {} to {} for user {}", 
                 startNoteId, endNoteId, userId);
        
        // Validate access to both notes
        validateNoteAccess(startNoteId, userId);
        validateNoteAccess(endNoteId, userId);
        
        if (startNoteId.equals(endNoteId)) {
            return List.of(validateNoteAccess(startNoteId, userId));
        }
        
        // Use BFS to find shortest path
        return breadthFirstSearch(startNoteId, endNoteId, userId, maxDepth);
    }
    
    /**
     * Calculate relationship strength between two notes
     */
    public double calculateRelationshipStrength(String sourceNoteId, String targetNoteId, String userId) {
        // Validate access
        Note sourceNote = validateNoteAccess(sourceNoteId, userId);
        Note targetNote = validateNoteAccess(targetNoteId, userId);
        
        double strength = 0.0;
        
        // Direct relationship bonus
        boolean hasDirectRelationship = sourceNote.getOutgoingLinks().stream()
            .anyMatch(link -> link.getTargetNote().getId().equals(targetNoteId));
        if (hasDirectRelationship) {
            strength += 0.5;
        }
        
        // Content similarity (using simple keyword overlap for now)
        strength += calculateContentSimilarity(sourceNote, targetNote);
        
        // Tag overlap
        strength += calculateTagSimilarity(sourceNote, targetNote);
        
        // Shared connections (mutual connections)
        strength += calculateSharedConnections(sourceNote, targetNote);
        
        return Math.min(1.0, strength);
    }
    
    /**
     * Suggest related notes for a given note
     */
    public List<NoteSuggestion> suggestRelatedNotes(String noteId, String userId, int limit) {
        log.debug("Finding suggested relationships for note {} and user {}", noteId, userId);
        
        Note sourceNote = validateNoteAccess(noteId, userId);
        List<Note> allUserNotes = noteRepository.findByCreatedByUserId(userId);
        
        return allUserNotes.stream()
            .filter(note -> !note.getId().equals(noteId))
            .filter(note -> !hasAnyRelationship(sourceNote, note))
            .map(note -> new NoteSuggestion(
                note,
                calculateRelationshipStrength(noteId, note.getId(), userId),
                suggestRelationshipType(sourceNote, note)
            ))
            .filter(suggestion -> suggestion.getStrength() > 0.2) // Threshold for suggestions
            .sorted((s1, s2) -> Double.compare(s2.getStrength(), s1.getStrength()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Find clusters of related notes
     */
    public List<NoteCluster> findNoteClusters(String userId, int minClusterSize) {
        log.debug("Finding note clusters for user {} with min size {}", userId, minClusterSize);
        
        List<Note> allNotes = noteRepository.findByCreatedByUserId(userId);
        
        // Simple clustering based on relationship density
        // In a more advanced implementation, this would use graph clustering algorithms
        List<NoteCluster> clusters = new ArrayList<>();
        Set<String> processedNotes = new HashSet<>();
        
        for (Note note : allNotes) {
            if (processedNotes.contains(note.getId())) {
                continue;
            }
            
            Set<Note> cluster = findConnectedComponents(note, allNotes, processedNotes);
            if (cluster.size() >= minClusterSize) {
                clusters.add(new NoteCluster(cluster, calculateClusterCohesion(cluster)));
            }
        }
        
        // Sort clusters by cohesion (quality metric)
        clusters.sort((c1, c2) -> Double.compare(c2.getCohesion(), c1.getCohesion()));
        
        return clusters;
    }
    
    /**
     * Get relationship analytics for a user's knowledge graph
     */
    public RelationshipAnalytics getRelationshipAnalytics(String userId) {
        List<Note> notes = noteRepository.findByCreatedByUserId(userId);
        
        int totalNotes = notes.size();
        int totalRelationships = notes.stream()
            .mapToInt(note -> note.getOutgoingLinks().size())
            .sum();
        
        double avgRelationshipsPerNote = totalNotes > 0 ? (double) totalRelationships / totalNotes : 0.0;
        
        // Find most connected notes
        List<Note> hubNotes = notes.stream()
            .sorted((n1, n2) -> Integer.compare(
                n2.getOutgoingLinks().size(), 
                n1.getOutgoingLinks().size()))
            .limit(10)
            .collect(Collectors.toList());
        
        // Relationship type distribution
        Map<String, Long> typeDistribution = notes.stream()
            .flatMap(note -> note.getOutgoingLinks().stream())
            .collect(Collectors.groupingBy(
                link -> link.getType() != null ? link.getType() : "unknown",
                Collectors.counting()
            ));
        
        return new RelationshipAnalytics(
            totalNotes,
            totalRelationships,
            avgRelationshipsPerNote,
            hubNotes,
            typeDistribution
        );
    }
    
    // Private helper methods
    
    private Note validateNoteAccess(String noteId, String userId) {
        Optional<Note> noteOpt = noteRepository.findById(noteId);
        if (noteOpt.isEmpty()) {
            throw new NoteNotFoundException(noteId);
        }
        
        Note note = noteOpt.get();
        if (!note.getCreatedBy().getId().equals(userId)) {
            throw new NoteAccessDeniedException(noteId, userId);
        }
        
        return note;
    }
    
    private String normalizeRelationshipType(String type) {
        return type != null ? type.toLowerCase().trim() : RELATED_TO;
    }
    
    private void validateRelationshipType(String type) {
        if (!VALID_RELATIONSHIP_TYPES.contains(type)) {
            throw new ValidationException("Invalid relationship type: " + type);
        }
    }
    
    private boolean hasExistingRelationship(Note sourceNote, String targetNoteId, String type) {
        return sourceNote.getOutgoingLinks().stream()
            .anyMatch(link -> 
                link.getTargetNote().getId().equals(targetNoteId) &&
                Objects.equals(link.getType(), type)
            );
    }
    
    private boolean wouldCreateCircularDependency(Note sourceNote, Note targetNote, String relationshipType) {
        // Only check for circular dependencies in hierarchical relationships
        Set<String> hierarchicalTypes = Set.of(PREREQUISITE, FOLLOWS_FROM, GENERALIZES, SPECIALIZES);
        
        if (!hierarchicalTypes.contains(relationshipType)) {
            return false;
        }
        
        // Check if target note already has a path back to source note
        return hasPathBetween(targetNote.getId(), sourceNote.getId(), sourceNote.getCreatedBy().getId(), 5);
    }
    
    private boolean hasPathBetween(String startId, String endId, String userId, int maxDepth) {
        List<Note> path = breadthFirstSearch(startId, endId, userId, maxDepth);
        return !path.isEmpty();
    }
    
    private NoteLink createEnhancedRelationship(Note sourceNote, Note targetNote, 
                                              LinkNotesRequest request, String relationshipType) {
        return NoteLink.builder()
            .targetNote(targetNote)
            .type(relationshipType)
            .context(request.getContext())
            .strength(calculateRelationshipStrength(sourceNote.getId(), targetNote.getId(), 
                     sourceNote.getCreatedBy().getId()))
            .createdAt(LocalDateTime.now())
            .createdBy(sourceNote.getCreatedBy())
            .build();
    }
    
    private List<Note> breadthFirstSearch(String startId, String endId, String userId, int maxDepth) {
        Queue<List<Note>> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        
        Note startNote = validateNoteAccess(startId, userId);
        queue.offer(List.of(startNote));
        visited.add(startId);
        
        while (!queue.isEmpty()) {
            List<Note> path = queue.poll();
            Note currentNote = path.get(path.size() - 1);
            
            if (path.size() > maxDepth) {
                continue;
            }
            
            // Check all outgoing relationships
            for (NoteLink link : currentNote.getOutgoingLinks()) {
                String nextId = link.getTargetNote().getId();
                
                if (nextId.equals(endId)) {
                    List<Note> resultPath = new ArrayList<>(path);
                    resultPath.add(link.getTargetNote());
                    return resultPath;
                }
                
                if (!visited.contains(nextId)) {
                    visited.add(nextId);
                    List<Note> newPath = new ArrayList<>(path);
                    newPath.add(link.getTargetNote());
                    queue.offer(newPath);
                }
            }
        }
        
        return Collections.emptyList();
    }
    
    private double calculateContentSimilarity(Note note1, Note note2) {
        // Simple keyword-based similarity (would be enhanced with NLP in production)
        String content1 = (note1.getContent() != null ? note1.getContent() : "").toLowerCase();
        String content2 = (note2.getContent() != null ? note2.getContent() : "").toLowerCase();
        
        Set<String> words1 = new HashSet<>(Arrays.asList(content1.split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(content2.split("\\s+")));
        
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size() * 0.3;
    }
    
    private double calculateTagSimilarity(Note note1, Note note2) {
        Set<String> tags1 = new HashSet<>(note1.getTags());
        Set<String> tags2 = new HashSet<>(note2.getTags());
        
        Set<String> intersection = new HashSet<>(tags1);
        intersection.retainAll(tags2);
        
        Set<String> union = new HashSet<>(tags1);
        union.addAll(tags2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size() * 0.2;
    }
    
    private double calculateSharedConnections(Note note1, Note note2) {
        Set<String> connections1 = note1.getOutgoingLinks().stream()
            .map(link -> link.getTargetNote().getId())
            .collect(Collectors.toSet());
            
        Set<String> connections2 = note2.getOutgoingLinks().stream()
            .map(link -> link.getTargetNote().getId())
            .collect(Collectors.toSet());
            
        Set<String> sharedConnections = new HashSet<>(connections1);
        sharedConnections.retainAll(connections2);
        
        int totalUniqueConnections = connections1.size() + connections2.size();
        return totalUniqueConnections > 0 ? (double) sharedConnections.size() / totalUniqueConnections : 0.0;
    }
    
    private boolean hasAnyRelationship(Note sourceNote, Note targetNote) {
        return sourceNote.getOutgoingLinks().stream()
            .anyMatch(link -> link.getTargetNote().getId().equals(targetNote.getId()));
    }
    
    private String suggestRelationshipType(Note sourceNote, Note targetNote) {
        // Simple heuristic-based relationship type suggestion
        // In production, this would use ML/NLP techniques
        
        double contentSimilarity = calculateContentSimilarity(sourceNote, targetNote);
        double tagSimilarity = calculateTagSimilarity(sourceNote, targetNote);
        
        if (contentSimilarity > 0.3 || tagSimilarity > 0.5) {
            return RELATED_TO;
        }
        
        return REFERENCES; // Default
    }
    
    private Set<Note> findConnectedComponents(Note startNote, List<Note> allNotes, Set<String> processed) {
        Set<Note> component = new HashSet<>();
        Queue<Note> queue = new LinkedList<>();
        
        queue.offer(startNote);
        component.add(startNote);
        processed.add(startNote.getId());
        
        while (!queue.isEmpty()) {
            Note current = queue.poll();
            
            // Find all connected notes
            for (NoteLink link : current.getOutgoingLinks()) {
                Note connected = link.getTargetNote();
                if (!processed.contains(connected.getId())) {
                    processed.add(connected.getId());
                    component.add(connected);
                    queue.offer(connected);
                }
            }
        }
        
        return component;
    }
    
    private double calculateClusterCohesion(Set<Note> cluster) {
        if (cluster.size() < 2) return 0.0;
        
        int totalRelationships = 0;
        int maxPossibleRelationships = cluster.size() * (cluster.size() - 1);
        
        for (Note note : cluster) {
            totalRelationships += note.getOutgoingLinks().stream()
                .mapToInt(link -> cluster.contains(link.getTargetNote()) ? 1 : 0)
                .sum();
        }
        
        return maxPossibleRelationships > 0 ? (double) totalRelationships / maxPossibleRelationships : 0.0;
    }
    
    // Inner classes for data transfer
    
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class NoteSuggestion {
        private Note note;
        private double strength;
        private String suggestedRelationshipType;
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class NoteCluster {
        private Set<Note> notes;
        private double cohesion;
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class RelationshipAnalytics {
        private int totalNotes;
        private int totalRelationships;
        private double averageRelationshipsPerNote;
        private List<Note> mostConnectedNotes;
        private Map<String, Long> relationshipTypeDistribution;
    }
}