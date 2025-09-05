package com.realm.service;

import com.realm.dto.GraphData;
import com.realm.dto.GraphEdge;
import com.realm.dto.GraphNode;
import com.realm.model.Note;
import com.realm.model.NoteLink;
import com.realm.repository.NoteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for creating graph visualization data from the Neo4j knowledge graph.
 * This service transforms the note relationships into format suitable for 
 * interactive graph visualization components.
 */
@Slf4j
@Service
public class GraphVisualizationService {
    
    @Autowired
    private Neo4jTemplate neo4jTemplate;
    
    @Autowired
    private NoteRepository noteRepository;
    
    /**
     * Get graph data for visualization with all user's notes and their relationships
     */
    public GraphData getGraphData(Long userId, Integer maxNodes) {
        log.debug("Fetching graph data for user {} with max nodes {}", userId, maxNodes);
        
        // Use repository to get all user's notes
        List<Note> notes = noteRepository.findByCreatedByUserId(userId);
        
        // Limit results if specified
        if (maxNodes != null && maxNodes > 0 && notes.size() > maxNodes) {
            notes = notes.subList(0, maxNodes);
        }
        
        // Convert to graph visualization format
        Set<GraphNode> nodeSet = new HashSet<>();
        Set<GraphEdge> edgeSet = new HashSet<>();
        
        // First pass: create all nodes
        for (Note note : notes) {
            int connectionCount = getConnectionCountForNote(note);
            GraphNode graphNode = createGraphNode(note, connectionCount);
            nodeSet.add(graphNode);
        }
        
        // Second pass: create edges from outgoing links
        for (Note note : notes) {
            String sourceId = note.getId().toString();
            
            if (note.getOutgoingLinks() != null) {
                for (NoteLink link : note.getOutgoingLinks()) {
                    if (link.getTargetNote() != null) {
                        String targetId = link.getTargetNote().getId().toString();
                        
                        // Only add edge if target note is in our node set
                        boolean targetExists = nodeSet.stream()
                            .anyMatch(node -> node.getId().equals(targetId));
                        
                        if (targetExists) {
                            GraphEdge edge = GraphEdge.builder()
                                .id(link.getId() != null ? link.getId().toString() : 
                                    sourceId + "_" + targetId)
                                .source(sourceId)
                                .target(targetId)
                                .type(link.getType() != null ? link.getType() : "REFERENCES")
                                .context(link.getContext())
                                .strength(link.getStrength() != null ? link.getStrength() : 1.0)
                                .color(getEdgeColor(link.getType()))
                                .width(calculateEdgeWidth(link.getStrength() != null ? 
                                    link.getStrength() : 1.0))
                                .build();
                            
                            edgeSet.add(edge);
                        }
                    }
                }
            }
        }
        
        GraphData graphData = GraphData.builder()
            .nodes(new ArrayList<>(nodeSet))
            .edges(new ArrayList<>(edgeSet))
            .build();
        
        graphData.calculateTotals();
        
        log.debug("Generated graph data with {} nodes and {} edges", 
                 graphData.getTotalNodes(), graphData.getTotalEdges());
        
        return graphData;
    }
    
    /**
     * Get a subgraph focused on a specific node and its connections
     */
    public GraphData getSubgraph(Long userId, String nodeId, Integer depth) {
        log.debug("Fetching subgraph for user {} centered on node {} with depth {}", 
                 userId, nodeId, depth);
        
        try {
            Long noteId = Long.parseLong(nodeId);
            int maxDepth = depth != null ? Math.min(depth, 3) : 2;
            
            // Use existing repository method to find related notes
            List<Note> relatedNotes = noteRepository.findRelatedNotes(noteId, userId, maxDepth, 50);
            
            // Find the center note
            Optional<Note> centerNote = noteRepository.findById(noteId);
            if (centerNote.isEmpty()) {
                return GraphData.builder()
                    .nodes(new ArrayList<>())
                    .edges(new ArrayList<>())
                    .centerNodeId(nodeId)
                    .build();
            }
            
            // Collect all notes (center + related)
            Set<Note> allNotes = new HashSet<>(relatedNotes);
            allNotes.add(centerNote.get());
            
            // Convert to graph visualization format
            Set<GraphNode> nodeSet = new HashSet<>();
            Set<GraphEdge> edgeSet = new HashSet<>();
            
            // Create nodes
            for (Note note : allNotes) {
                int connectionCount = getConnectionCountForNote(note);
                GraphNode graphNode = createGraphNode(note, connectionCount);
                if (note.getId().toString().equals(nodeId)) {
                    graphNode.setSelected(true); // Mark center node as selected
                }
                nodeSet.add(graphNode);
            }
            
            // Create edges from outgoing links
            for (Note note : allNotes) {
                String sourceId = note.getId().toString();
                
                if (note.getOutgoingLinks() != null) {
                    for (NoteLink link : note.getOutgoingLinks()) {
                        if (link.getTargetNote() != null) {
                            String targetId = link.getTargetNote().getId().toString();
                            
                            // Only add edge if target note is in our node set
                            boolean targetExists = nodeSet.stream()
                                .anyMatch(node -> node.getId().equals(targetId));
                            
                            if (targetExists) {
                                GraphEdge edge = GraphEdge.builder()
                                    .id(link.getId() != null ? link.getId().toString() : 
                                        sourceId + "_" + targetId)
                                    .source(sourceId)
                                    .target(targetId)
                                    .type(link.getType() != null ? link.getType() : "REFERENCES")
                                    .context(link.getContext())
                                    .strength(link.getStrength() != null ? link.getStrength() : 1.0)
                                    .color(getEdgeColor(link.getType()))
                                    .width(calculateEdgeWidth(link.getStrength() != null ? 
                                        link.getStrength() : 1.0))
                                    .build();
                                
                                edgeSet.add(edge);
                            }
                        }
                    }
                }
            }
            
            GraphData graphData = GraphData.builder()
                .nodes(new ArrayList<>(nodeSet))
                .edges(new ArrayList<>(edgeSet))
                .centerNodeId(nodeId)
                .build();
            
            graphData.calculateTotals();
            
            log.debug("Generated subgraph data with {} nodes and {} edges", 
                     graphData.getTotalNodes(), graphData.getTotalEdges());
            
            return graphData;
            
        } catch (NumberFormatException e) {
            log.error("Invalid nodeId format: {}", nodeId);
            return GraphData.builder()
                .nodes(new ArrayList<>())
                .edges(new ArrayList<>())
                .centerNodeId(nodeId)
                .build();
        }
    }
    
    /**
     * Search for nodes matching a query string
     */
    public List<GraphNode> searchNodes(Long userId, String query) {
        log.debug("Searching nodes for user {} with query '{}'", userId, query);
        
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        String searchTerm = query.toLowerCase().trim();
        
        // Get all user's notes and filter them
        List<Note> allNotes = noteRepository.findByCreatedByUserId(userId);
        
        return allNotes.stream()
            .filter(note -> note.containsSearchTerm(searchTerm))
            .sorted((n1, n2) -> {
                // Prioritize title matches, then tag matches, then content matches
                boolean n1TitleMatch = n1.getTitle() != null && 
                    n1.getTitle().toLowerCase().contains(searchTerm);
                boolean n2TitleMatch = n2.getTitle() != null && 
                    n2.getTitle().toLowerCase().contains(searchTerm);
                
                if (n1TitleMatch && !n2TitleMatch) return -1;
                if (n2TitleMatch && !n1TitleMatch) return 1;
                
                // If both or neither have title matches, sort by updated date
                return n2.getUpdatedAt().compareTo(n1.getUpdatedAt());
            })
            .limit(20)
            .map(note -> {
                int connectionCount = getConnectionCountForNote(note);
                GraphNode node = createGraphNode(note, connectionCount);
                node.setHighlighted(true); // Mark as search result
                return node;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Create a GraphNode from a Note entity
     */
    private GraphNode createGraphNode(Note note, int connectionCount) {
        return GraphNode.builder()
            .id(note.getId().toString())
            .title(note.getTitle())
            .content(truncateContent(note.getContent(), 100))
            .tags(note.getTags() != null ? new ArrayList<>(note.getTags()) : new ArrayList<>())
            .createdAt(note.getCreatedAt())
            .updatedAt(note.getUpdatedAt())
            .connectionCount(connectionCount)
            .size(calculateNodeSize(note, connectionCount))
            .color(getNodeColor(note))
            .build();
    }
    
    /**
     * Calculate node size based on content length and number of connections
     */
    private int calculateNodeSize(Note note, int connectionCount) {
        // Base size
        int baseSize = 30;
        
        // Size based on content length
        int contentSize = note.getContent() != null ? note.getContent().length() : 0;
        int contentBonus = Math.min(20, contentSize / 200); // Max 20px bonus for content
        
        // Size based on connections
        int connectionBonus = Math.min(15, connectionCount * 3); // Max 15px bonus for connections
        
        return baseSize + contentBonus + connectionBonus;
    }
    
    /**
     * Get node color based on tags or recency
     */
    private String getNodeColor(Note note) {
        // Color based on primary tag if available
        if (note.getTags() != null && !note.getTags().isEmpty()) {
            String primaryTag = note.getTags().get(0);
            return generateColorFromString(primaryTag);
        }
        
        // Color based on recency if no tags
        if (note.getCreatedAt() != null) {
            long daysSinceCreated = ChronoUnit.DAYS.between(
                note.getCreatedAt().toLocalDate(), 
                LocalDate.now()
            );
            
            if (daysSinceCreated < 7) return "#4CAF50"; // Green for recent (< 1 week)
            if (daysSinceCreated < 30) return "#2196F3"; // Blue for medium (< 1 month)
            if (daysSinceCreated < 90) return "#FF9800"; // Orange for older (< 3 months)
        }
        
        return "#9E9E9E"; // Gray for very old or no date
    }
    
    /**
     * Generate a consistent color from a string (for tags)
     */
    private String generateColorFromString(String input) {
        if (input == null || input.isEmpty()) {
            return "#9E9E9E";
        }
        
        // Predefined color palette for better visual consistency
        String[] colors = {
            "#FF6B6B", // Red
            "#4ECDC4", // Teal
            "#45B7D1", // Blue
            "#96CEB4", // Green
            "#FFEAA7", // Yellow
            "#DDA0DD", // Purple
            "#FFA07A", // Salmon
            "#87CEEB", // Sky Blue
            "#98FB98", // Pale Green
            "#F0E68C"  // Khaki
        };
        
        int hash = Math.abs(input.hashCode());
        return colors[hash % colors.length];
    }
    
    /**
     * Get edge color based on relationship type
     */
    private String getEdgeColor(String type) {
        if (type == null) return "#999999";
        
        switch (type) {
            case "REFERENCES": return "#999999";
            case "SUPPORTS": return "#4CAF50";
            case "CONTRADICTS": return "#F44336";
            case "BUILDS_ON": return "#2196F3";
            case "RELATED_TO": return "#9C27B0";
            case "INSPIRED_BY": return "#FF9800";
            case "CLARIFIES": return "#00BCD4";
            case "QUESTION": return "#CDDC39";
            case "ANSWER": return "#8BC34A";
            case "EXAMPLE": return "#795548";
            default: return "#999999";
        }
    }
    
    /**
     * Calculate edge width based on relationship strength
     */
    private double calculateEdgeWidth(double strength) {
        // Width between 1.0 and 4.0 based on strength
        return 1.0 + (strength * 3.0);
    }
    
    /**
     * Truncate content for preview
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        
        // Remove HTML tags for preview
        String plainText = content.replaceAll("<[^>]*>", "").trim();
        
        if (plainText.length() <= maxLength) {
            return plainText;
        }
        
        return plainText.substring(0, maxLength) + "...";
    }
    
    /**
     * Get connection count for a specific note using simple calculation
     */
    private int getConnectionCountForNote(Note note) {
        int count = 0;
        
        // Count outgoing links
        if (note.getOutgoingLinks() != null) {
            count += note.getOutgoingLinks().size();
        }
        
        // TODO: In the future, we can add a query to count incoming links
        // For now, we only count outgoing links to avoid relationship loading issues
        
        return count;
    }
    
    /**
     * Get connection count for a specific node (legacy method for compatibility)
     */
    private int getConnectionCount(String nodeId, Long userId) {
        // For now, return 0 for simplicity - this can be enhanced later
        return 0;
    }
}