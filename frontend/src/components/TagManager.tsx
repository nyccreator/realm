// TagManager component - manages tags for notes
// Provides interface for adding/removing tags and tag suggestions

import React, {useEffect, useRef, useState} from 'react';
import {Note} from '../types/note';
import {useNoteStore} from '../stores/noteStore';

interface TagManagerProps {
  note: Note;
  onTagsChange?: (tags: string[]) => void;
  className?: string;
  compact?: boolean;
}

export const TagManager: React.FC<TagManagerProps> = ({
  note,
  onTagsChange,
  className = '',
  compact = false
}) => {
  const {
    tags: allTags,
    addTagToCurrentNote,
    removeTagFromCurrentNote,
    loadTags
  } = useNoteStore();

  const [isAdding, setIsAdding] = useState(false);
  const [newTagInput, setNewTagInput] = useState('');
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [selectedSuggestionIndex, setSelectedSuggestionIndex] = useState(-1);
  
  const inputRef = useRef<HTMLInputElement>(null);

  // Load all tags on mount
  useEffect(() => {
    loadTags();
  }, [loadTags]);

  // Update suggestions based on input
  useEffect(() => {
    if (!newTagInput.trim()) {
      setSuggestions([]);
      setSelectedSuggestionIndex(-1);
      return;
    }

    const query = newTagInput.toLowerCase();
    const filtered = allTags
      .filter(tag => 
        tag.toLowerCase().includes(query) && 
        !note.tags.includes(tag)
      )
      .slice(0, 6);
    
    setSuggestions(filtered);
    setSelectedSuggestionIndex(-1);
  }, [newTagInput, allTags, note.tags]);

  // Focus input when adding mode starts
  useEffect(() => {
    if (isAdding && inputRef.current) {
      inputRef.current.focus();
    }
  }, [isAdding]);

  const handleAddTag = async (tagName: string) => {
    const trimmedTag = tagName.trim().toLowerCase();
    
    if (!trimmedTag || note.tags.includes(trimmedTag)) {
      return;
    }

    await addTagToCurrentNote(trimmedTag);
    
    // Reset form
    setNewTagInput('');
    setIsAdding(false);
    setSuggestions([]);
    setSelectedSuggestionIndex(-1);

    if (onTagsChange) {
      onTagsChange([...note.tags, trimmedTag]);
    }
  };

  const handleRemoveTag = async (tagName: string) => {
    await removeTagFromCurrentNote(tagName);
    
    if (onTagsChange) {
      onTagsChange(note.tags.filter(tag => tag !== tagName));
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      if (selectedSuggestionIndex >= 0 && suggestions[selectedSuggestionIndex]) {
        handleAddTag(suggestions[selectedSuggestionIndex]);
      } else if (newTagInput.trim()) {
        handleAddTag(newTagInput.trim());
      }
    } else if (e.key === 'Escape') {
      setIsAdding(false);
      setNewTagInput('');
      setSuggestions([]);
      setSelectedSuggestionIndex(-1);
    } else if (e.key === 'ArrowDown') {
      e.preventDefault();
      setSelectedSuggestionIndex(prev => 
        prev < suggestions.length - 1 ? prev + 1 : prev
      );
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setSelectedSuggestionIndex(prev => prev > -1 ? prev - 1 : prev);
    } else if (e.key === 'Tab') {
      if (selectedSuggestionIndex >= 0 && suggestions[selectedSuggestionIndex]) {
        e.preventDefault();
        setNewTagInput(suggestions[selectedSuggestionIndex]);
        setSelectedSuggestionIndex(-1);
      }
    }
  };

  const handleInputBlur = (e: React.FocusEvent<HTMLInputElement>) => {
    // Delay to allow suggestion clicks to register
    setTimeout(() => {
      if (newTagInput.trim()) {
        handleAddTag(newTagInput.trim());
      } else {
        setIsAdding(false);
        setNewTagInput('');
        setSuggestions([]);
        setSelectedSuggestionIndex(-1);
      }
    }, 200);
  };

  const handleSuggestionClick = (suggestion: string) => {
    handleAddTag(suggestion);
  };

  if (compact) {
    return (
      <div className={`flex flex-wrap gap-1 ${className}`}>
        {note.tags.map((tag) => (
          <span
            key={tag}
            className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-800 group hover:bg-blue-200"
          >
            #{tag}
            <button
              onClick={() => handleRemoveTag(tag)}
              className="ml-1 hover:text-blue-600 group-hover:text-blue-900"
              title="Remove tag"
            >
              <svg className="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </span>
        ))}
        
        <button
          onClick={() => setIsAdding(true)}
          className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-600 hover:bg-gray-200 transition-colors"
        >
          <svg className="h-3 w-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
          </svg>
          Tag
        </button>

        {isAdding && (
          <div className="relative">
            <input
              ref={inputRef}
              type="text"
              value={newTagInput}
              onChange={(e) => setNewTagInput(e.target.value)}
              onKeyDown={handleKeyDown}
              onBlur={handleInputBlur}
              placeholder="Add tag..."
              className="px-2 py-1 text-xs border border-blue-300 rounded-full focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              maxLength={50}
            />
            
            {suggestions.length > 0 && (
              <div className="absolute top-full left-0 mt-1 bg-white border border-gray-200 rounded-lg shadow-lg z-10 min-w-32">
                {suggestions.map((suggestion, index) => (
                  <button
                    key={suggestion}
                    onClick={() => handleSuggestionClick(suggestion)}
                    className={`w-full text-left px-3 py-2 text-xs hover:bg-gray-100 first:rounded-t-lg last:rounded-b-lg ${
                      index === selectedSuggestionIndex ? 'bg-blue-50 text-blue-900' : 'text-gray-700'
                    }`}
                  >
                    #{suggestion}
                  </button>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    );
  }

  return (
    <div className={`bg-white border rounded-lg ${className}`}>
      {/* Header */}
      <div className="px-4 py-3 border-b border-gray-200">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-medium text-gray-900">Tags</h3>
          <button
            onClick={() => setIsAdding(true)}
            className="inline-flex items-center px-2 py-1 text-xs font-medium text-blue-700 bg-blue-100 rounded hover:bg-blue-200 transition-colors"
          >
            <svg className="h-3 w-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
            </svg>
            Add Tag
          </button>
        </div>
      </div>

      {/* Content */}
      <div className="p-4">
        {/* Current Tags */}
        {note.tags.length > 0 ? (
          <div className="flex flex-wrap gap-2 mb-4">
            {note.tags.map((tag) => (
              <span
                key={tag}
                className="inline-flex items-center px-3 py-1.5 rounded-full text-sm font-medium bg-blue-100 text-blue-800 group hover:bg-blue-200 transition-colors"
              >
                #{tag}
                <button
                  onClick={() => handleRemoveTag(tag)}
                  className="ml-2 hover:text-blue-600 group-hover:text-blue-900 transition-colors"
                  title="Remove tag"
                >
                  <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </span>
            ))}
          </div>
        ) : (
          <p className="text-sm text-gray-500 italic mb-4">
            No tags added yet. Tags help organize and find your notes.
          </p>
        )}

        {/* Add Tag Input */}
        {isAdding && (
          <div className="relative">
            <input
              ref={inputRef}
              type="text"
              value={newTagInput}
              onChange={(e) => setNewTagInput(e.target.value)}
              onKeyDown={handleKeyDown}
              onBlur={handleInputBlur}
              placeholder="Enter tag name..."
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              maxLength={50}
            />
            
            {suggestions.length > 0 && (
              <div className="absolute top-full left-0 right-0 mt-1 bg-white border border-gray-200 rounded-lg shadow-lg z-10 max-h-48 overflow-y-auto">
                <div className="p-2 text-xs text-gray-500 border-b border-gray-100">
                  Suggestions:
                </div>
                {suggestions.map((suggestion, index) => (
                  <button
                    key={suggestion}
                    onClick={() => handleSuggestionClick(suggestion)}
                    className={`w-full text-left px-3 py-2 text-sm hover:bg-gray-100 ${
                      index === selectedSuggestionIndex ? 'bg-blue-50 text-blue-900' : 'text-gray-700'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span>#{suggestion}</span>
                      <span className="text-xs text-gray-400">
                        {allTags.filter(t => t === suggestion).length} notes
                      </span>
                    </div>
                  </button>
                ))}
              </div>
            )}
            
            <div className="mt-2 text-xs text-gray-500">
              Press Enter to add, Esc to cancel, Tab to autocomplete
            </div>
          </div>
        )}

        {/* Popular Tags */}
        {!isAdding && allTags.length > 0 && (
          <div>
            <h4 className="text-xs font-medium text-gray-700 mb-2">Popular Tags:</h4>
            <div className="flex flex-wrap gap-1">
              {allTags
                .filter(tag => !note.tags.includes(tag))
                .slice(0, 8)
                .map((tag) => (
                  <button
                    key={tag}
                    onClick={() => handleAddTag(tag)}
                    className="inline-flex items-center px-2 py-1 rounded text-xs font-medium bg-gray-100 text-gray-600 hover:bg-blue-100 hover:text-blue-700 transition-colors"
                  >
                    #{tag}
                  </button>
                ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};