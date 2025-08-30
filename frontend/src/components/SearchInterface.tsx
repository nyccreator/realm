// SearchInterface component - advanced search functionality for notes
// Provides full-text search, tag filtering, and advanced search options

import React, {useEffect, useRef, useState} from 'react';
import {Note} from '../types/note';
import {useNoteStore} from '../stores/noteStore';

interface SearchInterfaceProps {
  onResultSelect: (note: Note) => void;
  className?: string;
  placeholder?: string;
  showAdvanced?: boolean;
}

export const SearchInterface: React.FC<SearchInterfaceProps> = ({
  onResultSelect,
  className = '',
  placeholder = 'Search notes...',
  showAdvanced = true
}) => {
  const {
    notes,
    searchQuery,
    filters,
    tags: allTags,
    isLoading,
    searchNotes,
    setFilters,
    clearFilters
  } = useNoteStore();

  const [localQuery, setLocalQuery] = useState(searchQuery);
  const [showFilters, setShowFilters] = useState(false);
  const [searchResults, setSearchResults] = useState<Note[]>([]);
  const [showResults, setShowResults] = useState(false);
  
  const searchInputRef = useRef<HTMLInputElement>(null);
  const resultsRef = useRef<HTMLDivElement>(null);

  // Debounced search
  useEffect(() => {
    const timeoutId = setTimeout(() => {
      if (localQuery !== searchQuery) {
        searchNotes(localQuery);
      }
    }, 300);

    return () => clearTimeout(timeoutId);
  }, [localQuery, searchQuery, searchNotes]);

  // Update search results when notes change
  useEffect(() => {
    if (localQuery.trim()) {
      const filteredResults = notes.filter(note =>
        note.title.toLowerCase().includes(localQuery.toLowerCase()) ||
        note.content.toLowerCase().includes(localQuery.toLowerCase()) ||
        note.tags.some(tag => tag.toLowerCase().includes(localQuery.toLowerCase()))
      );
      setSearchResults(filteredResults);
      setShowResults(true);
    } else {
      setSearchResults([]);
      setShowResults(false);
    }
  }, [notes, localQuery]);

  // Close results when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        resultsRef.current &&
        !resultsRef.current.contains(event.target as Node) &&
        searchInputRef.current &&
        !searchInputRef.current.contains(event.target as Node)
      ) {
        setShowResults(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setLocalQuery(value);
    
    if (value.trim()) {
      setShowResults(true);
    } else {
      setShowResults(false);
    }
  };

  const handleResultSelect = (note: Note) => {
    onResultSelect(note);
    setShowResults(false);
    setLocalQuery('');
  };

  const handleTagFilter = (tag: string) => {
    const currentTags = filters.tags || [];
    const newTags = currentTags.includes(tag)
      ? currentTags.filter(t => t !== tag)
      : [...currentTags, tag];
    
    setFilters({ tags: newTags });
  };

  const clearSearch = () => {
    setLocalQuery('');
    setShowResults(false);
    clearFilters();
  };

  const getHighlightedText = (text: string, query: string) => {
    if (!query.trim()) return text;
    
    const parts = text.split(new RegExp(`(${query})`, 'gi'));
    return parts.map((part, index) =>
      part.toLowerCase() === query.toLowerCase() ? (
        <mark key={index} className="bg-yellow-200 text-yellow-900">
          {part}
        </mark>
      ) : (
        part
      )
    );
  };

  const getPreviewText = (content: string, query: string) => {
    const textContent = content.replace(/<[^>]*>/g, '');
    
    if (!query.trim()) {
      return textContent.length > 150 
        ? textContent.substring(0, 150) + '...' 
        : textContent;
    }

    // Find the query in the text and show context around it
    const index = textContent.toLowerCase().indexOf(query.toLowerCase());
    if (index === -1) {
      return textContent.length > 150 
        ? textContent.substring(0, 150) + '...' 
        : textContent;
    }

    const start = Math.max(0, index - 75);
    const end = Math.min(textContent.length, index + query.length + 75);
    const preview = textContent.substring(start, end);
    
    return (start > 0 ? '...' : '') + preview + (end < textContent.length ? '...' : '');
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffInMs = now.getTime() - date.getTime();
    const diffInDays = Math.floor(diffInMs / (1000 * 60 * 60 * 24));
    
    if (diffInDays === 0) {
      return 'Today';
    } else if (diffInDays === 1) {
      return 'Yesterday';
    } else if (diffInDays < 7) {
      return `${diffInDays} days ago`;
    } else {
      return date.toLocaleDateString([], {
        month: 'short',
        day: 'numeric'
      });
    }
  };

  return (
    <div className={`relative ${className}`}>
      {/* Search Input */}
      <div className="relative">
        <input
          ref={searchInputRef}
          type="text"
          placeholder={placeholder}
          value={localQuery}
          onChange={handleSearchChange}
          onFocus={() => localQuery.trim() && setShowResults(true)}
          className="w-full pl-10 pr-10 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        />
        
        {/* Search Icon */}
        <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
          <svg className="h-4 w-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
        </div>

        {/* Loading Indicator */}
        {isLoading && (
          <div className="absolute inset-y-0 right-8 flex items-center pointer-events-none">
            <div className="animate-spin h-4 w-4 border-2 border-blue-600 rounded-full border-t-transparent"></div>
          </div>
        )}

        {/* Clear Button */}
        {(localQuery || (filters.tags && filters.tags.length > 0)) && (
          <button
            onClick={clearSearch}
            className="absolute inset-y-0 right-0 pr-3 flex items-center hover:text-gray-700"
            title="Clear search"
          >
            <svg className="h-4 w-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        )}
      </div>

      {/* Advanced Filters */}
      {showAdvanced && (
        <div className="mt-2">
          <button
            onClick={() => setShowFilters(!showFilters)}
            className="text-xs text-gray-600 hover:text-gray-800 flex items-center"
          >
            <svg className={`h-3 w-3 mr-1 transform transition-transform ${showFilters ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
            </svg>
            Advanced filters
          </button>

          {showFilters && (
            <div className="mt-2 p-3 bg-gray-50 border border-gray-200 rounded-lg">
              {/* Tag Filters */}
              <div className="mb-3">
                <label className="block text-xs font-medium text-gray-700 mb-1">
                  Filter by tags:
                </label>
                <div className="flex flex-wrap gap-1">
                  {allTags.slice(0, 12).map((tag) => (
                    <button
                      key={tag}
                      onClick={() => handleTagFilter(tag)}
                      className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium transition-colors ${
                        filters.tags?.includes(tag)
                          ? 'bg-blue-100 text-blue-800 border border-blue-200'
                          : 'bg-white text-gray-600 border border-gray-200 hover:bg-gray-50'
                      }`}
                    >
                      #{tag}
                    </button>
                  ))}
                </div>
              </div>

              {/* Sort Options */}
              <div className="flex items-center space-x-4 text-xs">
                <label className="text-gray-700">Sort by:</label>
                <select
                  value={filters.sortBy || 'updatedAt'}
                  onChange={(e) => setFilters({ sortBy: e.target.value as any })}
                  className="border border-gray-300 rounded px-2 py-1 text-xs focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="updatedAt">Last updated</option>
                  <option value="createdAt">Created</option>
                  <option value="title">Title</option>
                </select>
                
                <button
                  onClick={() => setFilters({ sortOrder: filters.sortOrder === 'desc' ? 'asc' : 'desc' })}
                  className="p-1 text-gray-400 hover:text-gray-600 rounded transition-colors"
                  title={`Sort ${filters.sortOrder === 'desc' ? 'ascending' : 'descending'}`}
                >
                  <svg className={`h-3 w-3 transform ${filters.sortOrder === 'desc' ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 14l-7 7m0 0l-7-7m7 7V3" />
                  </svg>
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Applied Filters Display */}
      {filters.tags && filters.tags.length > 0 && (
        <div className="mt-2 flex flex-wrap gap-1">
          <span className="text-xs text-gray-500">Filtered by:</span>
          {filters.tags.map((tag) => (
            <span
              key={tag}
              className="inline-flex items-center px-2 py-0.5 rounded-full text-xs bg-blue-100 text-blue-800"
            >
              #{tag}
              <button
                onClick={() => handleTagFilter(tag)}
                className="ml-1 hover:text-blue-600"
              >
                <svg className="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </span>
          ))}
        </div>
      )}

      {/* Search Results */}
      {showResults && searchResults.length > 0 && (
        <div 
          ref={resultsRef}
          className="absolute top-full left-0 right-0 mt-1 bg-white border border-gray-200 rounded-lg shadow-lg z-50 max-h-96 overflow-y-auto"
        >
          <div className="p-2 text-xs text-gray-500 border-b border-gray-100">
            {searchResults.length} result{searchResults.length !== 1 ? 's' : ''} for "{localQuery}"
          </div>
          
          {searchResults.map((note) => (
            <button
              key={note.id}
              onClick={() => handleResultSelect(note)}
              className="w-full text-left p-3 hover:bg-gray-50 border-b border-gray-100 last:border-b-0 focus:outline-none focus:bg-blue-50"
            >
              <div className="flex items-start justify-between">
                <div className="flex-1 min-w-0">
                  <h4 className="text-sm font-medium text-gray-900 truncate">
                    {getHighlightedText(note.title, localQuery)}
                  </h4>
                  
                  <p className="text-xs text-gray-600 mt-1 line-clamp-2">
                    {getHighlightedText(getPreviewText(note.content, localQuery), localQuery)}
                  </p>
                  
                  <div className="flex items-center justify-between mt-2">
                    <div className="flex items-center space-x-2 text-xs text-gray-400">
                      <span>{formatDate(note.updatedAt)}</span>
                      {note.tags.length > 0 && (
                        <>
                          <span>•</span>
                          <div className="flex flex-wrap gap-1">
                            {note.tags.slice(0, 3).map((tag) => (
                              <span key={tag} className="text-blue-600">
                                #{tag}
                              </span>
                            ))}
                            {note.tags.length > 3 && (
                              <span>+{note.tags.length - 3}</span>
                            )}
                          </div>
                        </>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            </button>
          ))}
        </div>
      )}

      {/* No Results */}
      {showResults && searchResults.length === 0 && localQuery.trim() && (
        <div 
          ref={resultsRef}
          className="absolute top-full left-0 right-0 mt-1 bg-white border border-gray-200 rounded-lg shadow-lg z-50 p-4 text-center"
        >
          <svg className="mx-auto h-6 w-6 text-gray-300 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <p className="text-sm text-gray-500">No notes found for "{localQuery}"</p>
        </div>
      )}
    </div>
  );
};