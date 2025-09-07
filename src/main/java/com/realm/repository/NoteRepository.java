package com.realm.repository;

import com.realm.model.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Neo4j Repository for Note entity operations.
 * 
 * This repository provides graph-optimized queries for note management
 * in the PKM system. All queries leverage Neo4j's graph traversal capabilities
 * and are designed for optimal performance with the expected usage patterns.
 */
@Repository
public interface NoteRepository extends Neo4jRepository<Note, String> {
    
    /**
     * Find all notes created by a specific user with their outgoing relationships
     * Ordered by most recently updated first
     * This query fetches notes along with their outgoing links for graph visualization
     */
    @Query("MATCH (n:Note)-[:CREATED_BY]->(u:User) WHERE u.id = $userId " +
           "OPTIONAL MATCH (n)-[r:REFERENCES]->(target:Note) " +
           "RETURN n, collect(r), collect(target) ORDER BY n.updatedAt DESC")
    List<Note> findByCreatedByUserId(@Param("userId") String userId);
    
    /**
     * Find notes by user with pagination
     * Essential for performance with large note collections
     */
    @Query(value = "MATCH (n:Note)-[:CREATED_BY]->(u:User) WHERE u.id = $userId RETURN n ORDER BY n.updatedAt DESC",
           countQuery = "MATCH (n:Note)-[:CREATED_BY]->(u:User) WHERE u.id = $userId RETURN count(n)")
    Page<Note> findByCreatedByUserId(@Param("userId") String userId, Pageable pageable);
    
    /**
     * Find notes by status for a user
     * Leverages composite index for optimal performance
     */
    @Query("MATCH (n:Note)-[:CREATED_BY]->(u:User) WHERE u.id = $userId AND n.status = $status RETURN n ORDER BY n.updatedAt DESC")
    List<Note> findByCreatedByUserIdAndStatus(@Param("userId") String userId, @Param("status") String status);
    
    /**
     * Find favorite notes for a user
     * Optimized for quick access to starred content
     */
    @Query("MATCH (n:Note)-[:CREATED_BY]->(u:User) WHERE u.id = $userId AND n.isFavorite = true RETURN n ORDER BY n.updatedAt DESC")
    List<Note> findFavoritesByUserId(@Param("userId") String userId);
    
    /**
     * Find notes by category for a user
     */
    @Query("MATCH (n:Note)-[:CREATED_BY]->(u:User) WHERE u.id = $userId AND n.category = $category RETURN n ORDER BY n.updatedAt DESC")
    List<Note> findByCreatedByUserIdAndCategory(@Param("userId") String userId, @Param("category") String category);
    
    /**
     * Find notes containing specific tags
     * Uses array contains operation for tag matching
     */
    @Query("MATCH (n:Note)-[:CREATED_BY]->(u:User) WHERE u.id = $userId AND $tag IN n.tags RETURN n ORDER BY n.updatedAt DESC")
    List<Note> findByCreatedByUserIdAndTag(@Param("userId") String userId, @Param("tag") String tag);
    
    /**
     * Full-text search across note content
     * Leverages Neo4j's full-text indexes for optimal search performance
     */
    @Query("""
        CALL db.index.fulltext.queryNodes('note_content_fulltext', $searchTerm) YIELD node, score
        MATCH (node)-[:CREATED_BY]->(u:User) WHERE u.id = $userId
        RETURN node ORDER BY score DESC, node.updatedAt DESC
        """)
    List<Note> searchNotesByContent(@Param("userId") String userId, @Param("searchTerm") String searchTerm);
    
    /**
     * Find recently updated notes for a user
     * Useful for "recent activity" features
     */
    @Query("MATCH (n:Note)-[:CREATED_BY]->(u:User) WHERE u.id = $userId AND n.updatedAt >= $since RETURN n ORDER BY n.updatedAt DESC")
    List<Note> findRecentlyUpdatedByUserId(@Param("userId") String userId, @Param("since") LocalDateTime since);
    
    /**
     * Find notes that reference a specific note
     * Core functionality for backlink discovery
     */
    @Query("""
        MATCH (source:Note)-[r:REFERENCES]->(target:Note) WHERE target.id = $noteId
        MATCH (source)-[:CREATED_BY]->(u:User) WHERE u.id = $userId
        RETURN source, r ORDER BY r.createdAt DESC
        """)
    List<Note> findNotesReferencingNote(@Param("noteId") String noteId, @Param("userId") String userId);
    
    /**
     * Find notes referenced by a specific note
     * Essential for forward link traversal
     */
    @Query("""
        MATCH (source:Note)-[r:REFERENCES]->(target:Note) WHERE source.id = $noteId
        MATCH (source)-[:CREATED_BY]->(u:User) WHERE u.id = $userId
        RETURN target, r ORDER BY r.createdAt DESC
        """)
    List<Note> findNotesReferencedByNote(@Param("noteId") String noteId, @Param("userId") String userId);
    
    /**
     * Find related notes using graph traversal
     * Discovers notes connected within specified depth
     */
    @Query("""
        MATCH (start:Note)-[:CREATED_BY]->(u:User) WHERE start.id = $noteId AND id(u) = $userId
        MATCH path = (start)-[:REFERENCES*1..$depth]-(related:Note)
        WHERE related.id <> $noteId
        RETURN DISTINCT related, length(path) as distance
        ORDER BY distance ASC, related.updatedAt DESC
        LIMIT $limit
        """)
    List<Note> findRelatedNotes(@Param("noteId") String noteId, 
                               @Param("userId") String userId, 
                               @Param("depth") int depth, 
                               @Param("limit") int limit);
    
    /**
     * Get note statistics for a user
     * Provides counts and metrics for dashboard display
     */
    @Query("""
        MATCH (u:User) WHERE u.id = $userId
        OPTIONAL MATCH (u)<-[:CREATED_BY]-(n:Note)
        OPTIONAL MATCH (u)<-[:CREATED_BY]-(fn:Note {isFavorite: true})
        OPTIONAL MATCH (u)<-[:CREATED_BY]-(dn:Note {status: 'DRAFT'})
        OPTIONAL MATCH (u)<-[:CREATED_BY]-(pn:Note {status: 'PUBLISHED'})
        RETURN 
            count(n) as totalNotes,
            count(fn) as favoriteNotes,
            count(dn) as draftNotes,
            count(pn) as publishedNotes,
            max(n.updatedAt) as lastNoteUpdate
        """)
    NoteStatistics getNoteStatistics(@Param("userId") String userId);
    
    /**
     * Find orphaned notes (notes with no incoming or outgoing links)
     * Useful for identifying isolated content
     */
    @Query("""
        MATCH (n:Note)-[:CREATED_BY]->(u:User) WHERE u.id = $userId
        AND NOT (n)-[:REFERENCES]-() AND NOT ()-[:REFERENCES]->(n)
        RETURN n ORDER BY n.updatedAt DESC
        """)
    List<Note> findOrphanedNotes(@Param("userId") String userId);
    
    /**
     * Find highly connected notes (hub notes)
     * Identifies important nodes in the knowledge graph
     */
    @Query("""
        MATCH (n:Note)-[:CREATED_BY]->(u:User) WHERE u.id = $userId
        OPTIONAL MATCH (n)-[out:REFERENCES]->()
        OPTIONAL MATCH ()-[in:REFERENCES]->(n)
        WITH n, count(out) + count(in) as connectionCount
        WHERE connectionCount >= $minConnections
        RETURN n, connectionCount ORDER BY connectionCount DESC, n.updatedAt DESC
        """)
    List<Note> findHighlyConnectedNotes(@Param("userId") String userId, @Param("minConnections") int minConnections);
    
    /**
     * Update note's last accessed timestamp
     * Lightweight update for usage tracking
     */
    @Query("MATCH (n:Note) WHERE n.id = $noteId SET n.lastAccessedAt = $timestamp RETURN n")
    Optional<Note> updateLastAccessed(@Param("noteId") String noteId, @Param("timestamp") LocalDateTime timestamp);
    
    /**
     * Interface for note statistics projection
     */
    interface NoteStatistics {
        Long getTotalNotes();
        Long getFavoriteNotes();
        Long getDraftNotes();
        Long getPublishedNotes();
        LocalDateTime getLastNoteUpdate();
    }
}