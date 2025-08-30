// Graph Search Component - Search functionality with highlighting
// Allows users to search for nodes and highlights matching results

import React, {useCallback, useEffect, useRef, useState} from 'react';
import {GraphNode} from '../../types/graph';

interface GraphSearchProps {
  query: string;
  onQueryChange: (query: string) => void;
  isSearching: boolean;
  results?: GraphNode[];
}

export const GraphSearch: React.FC<GraphSearchProps> = ({
  query,
  onQueryChange,
  isSearching,
  results = []
}) => {
  const [isFocused, setIsFocused] = useState(false);
  const [showResults, setShowResults] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const resultsRef = useRef<HTMLDivElement>(null);

  // Show results when we have results and the input is focused
  useEffect(() => {
    setShowResults(isFocused && results.length > 0 && query.length > 0);
  }, [isFocused, results.length, query.length]);

  // Handle keyboard shortcuts
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      // Focus search on Ctrl/Cmd + K
      if ((event.metaKey || event.ctrlKey) && event.key === 'k') {
        event.preventDefault();
        inputRef.current?.focus();
      }
      // Clear search on Escape
      if (event.key === 'Escape' && isFocused) {
        onQueryChange('');
        inputRef.current?.blur();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isFocused, onQueryChange]);

  const handleInputChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    onQueryChange(event.target.value);
  }, [onQueryChange]);

  const handleInputFocus = useCallback(() => {
    setIsFocused(true);
  }, []);

  const handleInputBlur = useCallback(() => {
    // Delay hiding results to allow for result clicks
    setTimeout(() => setIsFocused(false), 200);
  }, []);

  const handleClearSearch = useCallback(() => {
    onQueryChange('');
    inputRef.current?.focus();
  }, [onQueryChange]);

  const handleResultClick = useCallback((node: GraphNode) => {
    // You could emit a custom event or call a prop function here
    // to center the view on the clicked node
    console.log('Selected search result:', node);
  }, []);

  const getHighlightedText = (text: string, highlight: string) => {
    if (!highlight.trim()) return text;
    
    const regex = new RegExp(`(${highlight.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi');
    const parts = text.split(regex);
    
    return parts.map((part, i) => (
      regex.test(part) ? (
        <mark key={i} className="bg-yellow-200 text-yellow-800 px-1 rounded">
          {part}
        </mark>
      ) : (
        part
      )
    ));
  };

  return (
    <div className="relative w-full">
      {/* Search Input */}
      <div className="relative">
        <div className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none">
          {isSearching ? (
            <svg className="w-4 h-4 text-gray-400 animate-spin" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
          ) : (
            <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          )}
        </div>
        
        <input
          ref={inputRef}
          type="text"
          value={query}
          onChange={handleInputChange}
          onFocus={handleInputFocus}
          onBlur={handleInputBlur}
          placeholder="Search nodes... (Ctrl+K)"
          className="w-full pl-10 pr-10 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        />
        
        {query && (
          <button
            onClick={handleClearSearch}
            className="absolute inset-y-0 right-0 flex items-center pr-3 text-gray-400 hover:text-gray-600"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        )}
      </div>

      {/* Search Results */}
      {showResults && (
        <div 
          ref={resultsRef}
          className="absolute top-full left-0 right-0 mt-1 bg-white border border-gray-200 rounded-md shadow-lg z-50 max-h-64 overflow-y-auto"
        >
          <div className="p-2">
            <div className="text-xs text-gray-500 mb-2 px-2">
              {results.length} result{results.length !== 1 ? 's' : ''} found
            </div>
            
            {results.map((node, index) => (
              <button
                key={node.id}
                onClick={() => handleResultClick(node)}
                className="w-full text-left p-2 rounded hover:bg-gray-50 focus:bg-gray-50 focus:outline-none transition-colors"
              >
                <div className="flex items-center space-x-3">
                  {/* Node color indicator */}
                  <div 
                    className="w-3 h-3 rounded-full flex-shrink-0"
                    style={{ backgroundColor: node.color }}
                  />
                  
                  <div className="flex-1 min-w-0">
                    <div className="font-medium text-sm text-gray-900 truncate">
                      {getHighlightedText(node.title, query)}
                    </div>
                    
                    {node.tags.length > 0 && (
                      <div className="flex flex-wrap gap-1 mt-1">
                        {node.tags.slice(0, 3).map((tag, tagIndex) => (
                          <span
                            key={tagIndex}
                            className="inline-block bg-gray-100 text-gray-600 px-1.5 py-0.5 rounded text-xs"
                          >
                            {getHighlightedText(tag, query)}
                          </span>
                        ))}
                        {node.tags.length > 3 && (
                          <span className="text-xs text-gray-400">
                            +{node.tags.length - 3} more
                          </span>
                        )}
                      </div>
                    )}
                    
                    <div className="text-xs text-gray-500 mt-1 flex items-center space-x-2">
                      <span>{node.connectionCount} connection{node.connectionCount !== 1 ? 's' : ''}</span>
                      {node.updatedAt && (
                        <>
                          <span>•</span>
                          <span>
                            Updated {new Date(node.updatedAt).toLocaleDateString()}
                          </span>
                        </>
                      )}
                    </div>
                  </div>

                  {/* Connection indicator */}
                  {node.connectionCount > 0 && (
                    <div className="flex-shrink-0">
                      <div className="bg-blue-100 text-blue-800 text-xs px-2 py-1 rounded-full font-medium">
                        {node.connectionCount}
                      </div>
                    </div>
                  )}
                </div>
              </button>
            ))}
          </div>

          {/* Search Tips */}
          {results.length === 0 && query.length > 0 && !isSearching && (
            <div className="p-4 text-center text-gray-500">
              <div className="text-sm mb-2">No matching nodes found</div>
              <div className="text-xs">
                Try different keywords or check your spelling
              </div>
            </div>
          )}
        </div>
      )}

      {/* Search Stats */}
      {query && results.length > 0 && (
        <div className="text-xs text-gray-500 mt-2 flex items-center justify-between">
          <span>
            Showing {results.length} match{results.length !== 1 ? 'es' : ''} for "{query}"
          </span>
          <span className="text-xs text-gray-400">
            Press Esc to clear
          </span>
        </div>
      )}
    </div>
  );
};