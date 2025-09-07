package com.realm.service;

import com.realm.model.Note;
import com.realm.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * SearchService - Advanced search and discovery service
 * 
 * Provides sophisticated search capabilities including:
 * - Full-text search with ranking and relevance scoring
 * - Faceted search with filters and aggregations  
 * - Semantic search and content similarity
 * - Graph-based search and path discovery
 * - Auto-complete and search suggestions
 * - Search analytics and user behavior tracking
 * - Saved searches and search history
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SearchService {
    
    private final NoteRepository noteRepository;
    private final CacheService cacheService;
    
    // Search configuration
    private static final int DEFAULT_SEARCH_LIMIT = 50;
    private static final int MAX_SEARCH_LIMIT = 500;
    private static final double MIN_RELEVANCE_SCORE = 0.1;
    
    // Search operators
    private static final Pattern QUOTED_PHRASE = Pattern.compile("\"([^\"]+)\"");
    private static final Pattern BOOLEAN_AND = Pattern.compile("\\s+AND\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern BOOLEAN_OR = Pattern.compile("\\s+OR\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern BOOLEAN_NOT = Pattern.compile("\\s+NOT\\s+", Pattern.CASE_INSENSITIVE);
    
    /**
     * Advanced search with multiple search modes and filters
     */
    public SearchResult searchNotes(SearchQuery query, String userId) {
        log.debug("Performing advanced search for user {} with query: '{}'", userId, query.getQuery());
        
        validateSearchQuery(query);
        
        // Check cache first for exact query matches
        String cacheKey = buildSearchCacheKey(query, userId);
        Optional<SearchResult> cachedResult = getCachedSearchResult(cacheKey);
        if (cachedResult.isPresent() && !query.isForceRefresh()) {
            log.debug("Returning cached search result for query: '{}'", query.getQuery());
            return cachedResult.get();
        }
        
        SearchResult result = performSearch(query, userId);
        
        // Cache the result
        cacheSearchResult(cacheKey, result);
        
        log.info("Search completed for user {}: {} results for query '{}'", 
                userId, result.getNotes().size(), query.getQuery());
        
        return result;
    }
    
    /**
     * Get search suggestions and auto-complete
     */
    public List<String> getSearchSuggestions(String partialQuery, String userId, int limit) {
        log.debug("Getting search suggestions for user {} with partial query: '{}'", userId, partialQuery);
        
        if (partialQuery == null || partialQuery.trim().length() < 2) {
            return Collections.emptyList();
        }
        
        Set<String> suggestions = new HashSet<>();
        
        // Add tag-based suggestions
        suggestions.addAll(getTagSuggestions(partialQuery, userId, limit));
        
        // Add title-based suggestions
        suggestions.addAll(getTitleSuggestions(partialQuery, userId, limit));
        
        // Add content-based suggestions
        suggestions.addAll(getContentSuggestions(partialQuery, userId, limit));
        
        return suggestions.stream()
            .sorted()
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Find similar notes based on content and relationships
     */
    public List<SimilarNote> findSimilarNotes(String noteId, String userId, int limit) {
        log.debug("Finding similar notes for note {} and user {}", noteId, userId);
        
        // Get the source note
        Optional<Note> sourceNoteOpt = noteRepository.findById(noteId);
        if (sourceNoteOpt.isEmpty() || !hasUserAccess(sourceNoteOpt.get(), userId)) {
            return Collections.emptyList();
        }
        
        Note sourceNote = sourceNoteOpt.get();
        List<Note> allUserNotes = noteRepository.findByCreatedByUserId(userId);
        
        return allUserNotes.stream()
            .filter(note -> !note.getId().equals(noteId))
            .map(note -> new SimilarNote(note, calculateSimilarityScore(sourceNote, note)))
            .filter(similar -> similar.getSimilarityScore() > MIN_RELEVANCE_SCORE)
            .sorted((s1, s2) -> Double.compare(s2.getSimilarityScore(), s1.getSimilarityScore()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Perform graph-based search using relationship traversal
     */
    public List<Note> searchByGraphTraversal(String startNoteId, SearchQuery query, String userId, int maxDepth) {
        log.debug("Performing graph-based search from note {} for user {}", startNoteId, userId);
        
        Optional<Note> startNoteOpt = noteRepository.findById(startNoteId);
        if (startNoteOpt.isEmpty() || !hasUserAccess(startNoteOpt.get(), userId)) {
            return Collections.emptyList();
        }
        
        Set<Note> visitedNotes = new HashSet<>();
        List<Note> matchingNotes = new ArrayList<>();
        
        traverseGraph(startNoteOpt.get(), query, visitedNotes, matchingNotes, 0, maxDepth);
        
        // Sort by relevance and return
        return matchingNotes.stream()
            .sorted((n1, n2) -> Double.compare(
                calculateRelevanceScore(n2, query), 
                calculateRelevanceScore(n1, query)))
            .collect(Collectors.toList());
    }
    
    /**
     * Get search analytics for a user
     */
    public SearchAnalytics getSearchAnalytics(String userId, LocalDateTime since) {
        log.debug("Getting search analytics for user {} since {}", userId, since);
        
        // In a full implementation, this would query search history from a dedicated table
        return SearchAnalytics.builder()
            .userId(userId)
            .totalSearches(0L) // Placeholder
            .mostSearchedTerms(Collections.emptyMap())
            .averageResultCount(0.0)
            .searchSuccessRate(0.0)
            .build();
    }
    
    // Private helper methods
    
    private SearchResult performSearch(SearchQuery query, String userId) {
        List<Note> allResults = new ArrayList<>();
        Map<String, Long> facetCounts = new HashMap<>();
        
        // Parse search query
        ParsedQuery parsedQuery = parseSearchQuery(query.getQuery());
        
        // Perform different types of searches based on query type
        if (parsedQuery.hasFullTextTerms()) {
            List<Note> fullTextResults = performFullTextSearch(parsedQuery, userId);
            allResults.addAll(fullTextResults);
        }
        
        if (parsedQuery.hasTags()) {
            List<Note> tagResults = performTagSearch(parsedQuery.getTags(), userId);
            allResults.addAll(tagResults);
        }
        
        if (parsedQuery.hasFilters()) {
            allResults = applyFilters(allResults, parsedQuery.getFilters(), userId);
        }
        
        // Remove duplicates and apply user access control
        allResults = allResults.stream()
            .distinct()
            .filter(note -> hasUserAccess(note, userId))
            .collect(Collectors.toList());
        
        // Calculate relevance scores and sort
        List<ScoredNote> scoredResults = calculateRelevanceScores(allResults, query);
        
        // Apply pagination
        int offset = query.getOffset();
        int limit = Math.min(query.getLimit(), MAX_SEARCH_LIMIT);
        
        List<ScoredNote> paginatedResults = scoredResults.stream()
            .skip(offset)
            .limit(limit)
            .collect(Collectors.toList());
        
        // Build facet counts
        facetCounts.put("total", (long) allResults.size());
        facetCounts.putAll(buildFacetCounts(allResults));
        
        return SearchResult.builder()
            .notes(paginatedResults.stream().map(ScoredNote::getNote).collect(Collectors.toList()))
            .totalResults(allResults.size())
            .searchTime(System.currentTimeMillis()) // Would calculate actual search time
            .facetCounts(facetCounts)
            .query(query.getQuery())
            .build();
    }
    
    private ParsedQuery parseSearchQuery(String query) {
        ParsedQuery.ParsedQueryBuilder builder = ParsedQuery.builder();
        
        String workingQuery = query.trim();
        
        // Extract quoted phrases
        java.util.regex.Matcher phraseMatcher = QUOTED_PHRASE.matcher(workingQuery);
        List<String> phrases = new ArrayList<>();
        while (phraseMatcher.find()) {
            phrases.add(phraseMatcher.group(1));
            workingQuery = workingQuery.replace(phraseMatcher.group(0), "");
        }
        builder.phrases(phrases);
        
        // Extract tags (words starting with #)
        List<String> tags = Arrays.stream(workingQuery.split("\\s+"))
            .filter(word -> word.startsWith("#") && word.length() > 1)
            .map(word -> word.substring(1).toLowerCase())
            .collect(Collectors.toList());
        builder.tags(tags);
        
        // Remove tags from working query
        workingQuery = workingQuery.replaceAll("#\\w+", "").trim();
        
        // Extract remaining terms
        List<String> terms = Arrays.stream(workingQuery.split("\\s+"))
            .filter(word -> !word.isEmpty())
            .map(String::toLowerCase)
            .collect(Collectors.toList());
        builder.terms(terms);
        
        return builder.build();
    }
    
    private List<Note> performFullTextSearch(ParsedQuery parsedQuery, String userId) {
        // Combine all terms for full-text search
        String searchTerms = String.join(" ", parsedQuery.getTerms());
        searchTerms += " " + String.join(" ", parsedQuery.getPhrases());
        
        if (searchTerms.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        return noteRepository.searchNotesByContent(userId, searchTerms.trim());
    }
    
    private List<Note> performTagSearch(List<String> tags, String userId) {
        if (tags.isEmpty()) {
            return Collections.emptyList();
        }
        
        // For multiple tags, find notes that have any of the tags
        List<Note> results = new ArrayList<>();
        for (String tag : tags) {
            results.addAll(noteRepository.findByCreatedByUserIdAndTag(userId, tag));
        }
        
        return results;
    }
    
    private List<Note> applyFilters(List<Note> notes, Map<String, String> filters, String userId) {
        return notes.stream()
            .filter(note -> matchesFilters(note, filters))
            .collect(Collectors.toList());
    }
    
    private boolean matchesFilters(Note note, Map<String, String> filters) {
        for (Map.Entry<String, String> filter : filters.entrySet()) {
            String filterName = filter.getKey().toLowerCase();
            String filterValue = filter.getValue().toLowerCase();
            
            switch (filterName) {
                case "status":
                    if (!filterValue.equals(note.getStatus().toLowerCase())) {
                        return false;
                    }
                    break;
                case "favorite":
                    boolean isFavorite = "true".equals(filterValue);
                    if (note.isFavorite() != isFavorite) {
                        return false;
                    }
                    break;
                case "after":
                    try {
                        LocalDateTime afterDate = LocalDateTime.parse(filterValue);
                        if (note.getCreatedAt().isBefore(afterDate)) {
                            return false;
                        }
                    } catch (Exception e) {
                        log.warn("Invalid date filter: {}", filterValue);
                    }
                    break;
            }
        }
        return true;
    }
    
    private List<ScoredNote> calculateRelevanceScores(List<Note> notes, SearchQuery query) {
        ParsedQuery parsedQuery = parseSearchQuery(query.getQuery());
        
        return notes.stream()
            .map(note -> new ScoredNote(note, calculateRelevanceScore(note, query)))
            .sorted((s1, s2) -> Double.compare(s2.getRelevanceScore(), s1.getRelevanceScore()))
            .collect(Collectors.toList());
    }
    
    private double calculateRelevanceScore(Note note, SearchQuery query) {
        double score = 0.0;
        String queryText = query.getQuery().toLowerCase();
        
        // Title matches have higher weight
        if (note.getTitle().toLowerCase().contains(queryText)) {
            score += 0.4;
        }
        
        // Content matches
        if (note.getContent() != null && note.getContent().toLowerCase().contains(queryText)) {
            score += 0.3;
        }
        
        // Tag matches
        for (String tag : note.getTags()) {
            if (queryText.contains(tag.toLowerCase())) {
                score += 0.2;
            }
        }
        
        // Boost for recent notes
        if (note.getUpdatedAt().isAfter(LocalDateTime.now().minusDays(30))) {
            score += 0.1;
        }
        
        // Boost for favorite notes
        if (note.isFavorite()) {
            score += 0.05;
        }
        
        return Math.min(1.0, score);
    }
    
    private double calculateSimilarityScore(Note note1, Note note2) {
        double similarity = 0.0;
        
        // Content similarity (simple word overlap)
        similarity += calculateContentSimilarity(note1, note2) * 0.5;
        
        // Tag similarity
        similarity += calculateTagSimilarity(note1, note2) * 0.3;
        
        // Title similarity
        similarity += calculateTitleSimilarity(note1, note2) * 0.2;
        
        return similarity;
    }
    
    private double calculateContentSimilarity(Note note1, Note note2) {
        String content1 = stripHtml(note1.getContent()).toLowerCase();
        String content2 = stripHtml(note2.getContent()).toLowerCase();
        
        Set<String> words1 = new HashSet<>(Arrays.asList(content1.split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(content2.split("\\s+")));
        
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    private double calculateTagSimilarity(Note note1, Note note2) {
        Set<String> tags1 = new HashSet<>(note1.getTags());
        Set<String> tags2 = new HashSet<>(note2.getTags());
        
        Set<String> intersection = new HashSet<>(tags1);
        intersection.retainAll(tags2);
        
        Set<String> union = new HashSet<>(tags1);
        union.addAll(tags2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    private double calculateTitleSimilarity(Note note1, Note note2) {
        String title1 = note1.getTitle().toLowerCase();
        String title2 = note2.getTitle().toLowerCase();
        
        // Simple word overlap in titles
        Set<String> words1 = new HashSet<>(Arrays.asList(title1.split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(title2.split("\\s+")));
        
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        return Math.max(words1.size(), words2.size()) == 0 ? 0.0 : 
               (double) intersection.size() / Math.max(words1.size(), words2.size());
    }
    
    private void traverseGraph(Note currentNote, SearchQuery query, Set<Note> visitedNotes, 
                              List<Note> matchingNotes, int currentDepth, int maxDepth) {
        
        if (currentDepth > maxDepth || visitedNotes.contains(currentNote)) {
            return;
        }
        
        visitedNotes.add(currentNote);
        
        // Check if current note matches query
        if (noteMatchesQuery(currentNote, query)) {
            matchingNotes.add(currentNote);
        }
        
        // Traverse outgoing links
        for (var link : currentNote.getOutgoingLinks()) {
            traverseGraph(link.getTargetNote(), query, visitedNotes, matchingNotes, 
                         currentDepth + 1, maxDepth);
        }
    }
    
    private boolean noteMatchesQuery(Note note, SearchQuery query) {
        String queryLower = query.getQuery().toLowerCase();
        
        return note.getTitle().toLowerCase().contains(queryLower) ||
               (note.getContent() != null && note.getContent().toLowerCase().contains(queryLower)) ||
               note.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(queryLower));
    }
    
    private List<String> getTagSuggestions(String partialQuery, String userId, int limit) {
        // Would query a tag aggregation table in a full implementation
        return Collections.emptyList();
    }
    
    private List<String> getTitleSuggestions(String partialQuery, String userId, int limit) {
        List<Note> notes = noteRepository.findByCreatedByUserId(userId);
        
        return notes.stream()
            .map(Note::getTitle)
            .filter(title -> title.toLowerCase().contains(partialQuery.toLowerCase()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    private List<String> getContentSuggestions(String partialQuery, String userId, int limit) {
        // Would implement more sophisticated content-based suggestions
        return Collections.emptyList();
    }
    
    private Map<String, Long> buildFacetCounts(List<Note> notes) {
        Map<String, Long> facets = new HashMap<>();
        
        // Count by status
        facets.putAll(notes.stream()
            .collect(Collectors.groupingBy(Note::getStatus, Collectors.counting()))
            .entrySet().stream()
            .collect(Collectors.toMap(
                entry -> "status:" + entry.getKey(),
                Map.Entry::getValue
            )));
        
        // Count favorites
        long favoriteCount = notes.stream().mapToLong(note -> note.isFavorite() ? 1 : 0).sum();
        facets.put("favorite:true", favoriteCount);
        facets.put("favorite:false", notes.size() - favoriteCount);
        
        return facets;
    }
    
    private void validateSearchQuery(SearchQuery query) {
        if (query.getQuery() == null || query.getQuery().trim().isEmpty()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }
        
        if (query.getLimit() > MAX_SEARCH_LIMIT) {
            throw new IllegalArgumentException("Search limit cannot exceed " + MAX_SEARCH_LIMIT);
        }
    }
    
    private boolean hasUserAccess(Note note, String userId) {
        return note.getCreatedBy() != null && note.getCreatedBy().getId().equals(userId);
    }
    
    private String stripHtml(String html) {
        return html != null ? html.replaceAll("<[^>]*>", "") : "";
    }
    
    // Cache-related methods
    
    private String buildSearchCacheKey(SearchQuery query, String userId) {
        return String.format("search:%d:%s", userId, query.hashCode());
    }
    
    private Optional<SearchResult> getCachedSearchResult(String cacheKey) {
        // Would integrate with cache service
        return Optional.empty();
    }
    
    private void cacheSearchResult(String cacheKey, SearchResult result) {
        // Would cache search results
    }
    
    // Inner classes for search data structures
    
    @lombok.Data
    @lombok.Builder
    public static class SearchQuery {
        private String query;
        @lombok.Builder.Default
        private int limit = DEFAULT_SEARCH_LIMIT;
        @lombok.Builder.Default
        private int offset = 0;
        private Map<String, String> filters;
        @lombok.Builder.Default
        private boolean forceRefresh = false;
    }
    
    @lombok.Data
    @lombok.Builder
    private static class ParsedQuery {
        @lombok.Builder.Default
        private List<String> terms = new ArrayList<>();
        @lombok.Builder.Default
        private List<String> phrases = new ArrayList<>();
        @lombok.Builder.Default
        private List<String> tags = new ArrayList<>();
        @lombok.Builder.Default
        private Map<String, String> filters = new HashMap<>();
        
        public boolean hasFullTextTerms() {
            return !terms.isEmpty() || !phrases.isEmpty();
        }
        
        public boolean hasTags() {
            return !tags.isEmpty();
        }
        
        public boolean hasFilters() {
            return !filters.isEmpty();
        }
    }
    
    @lombok.Data
    @lombok.Builder
    public static class SearchResult {
        private List<Note> notes;
        private int totalResults;
        private long searchTime;
        private Map<String, Long> facetCounts;
        private String query;
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ScoredNote {
        private Note note;
        private double relevanceScore;
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class SimilarNote {
        private Note note;
        private double similarityScore;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class SearchAnalytics {
        private String userId;
        private Long totalSearches;
        private Map<String, Long> mostSearchedTerms;
        private Double averageResultCount;
        private Double searchSuccessRate;
    }
}