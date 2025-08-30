// NoteList component - displays and manages the list of notes
// Includes search, filtering, and note selection functionality

import React, {useEffect, useState} from 'react';
import {Note} from '../types/note';
import {useNoteStore} from '../stores/noteStore';

interface NoteListProps {
  onNoteSelect: (note: Note) => void;
  selectedNoteId?: string | null;
}

export const NoteList: React.FC<NoteListProps> = ({ 
  onNoteSelect, 
  selectedNoteId 
}) => {
  const {
    notes,
    isLoading,
    error,
    searchQuery,
    filters,
    loadNotes,
    createNote,
    deleteNote,
    searchNotes,
    setFilters,
    clearFilters,
    clearError
  } = useNoteStore();

  const [localSearchQuery, setLocalSearchQuery] = useState(searchQuery);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [newNoteTitle, setNewNoteTitle] = useState('');

  // Load notes on mount
  useEffect(() => {
    loadNotes();
  }, [loadNotes]);

  // Debounced search
  useEffect(() => {
    const timeoutId = setTimeout(() => {
      if (localSearchQuery !== searchQuery) {
        searchNotes(localSearchQuery);
      }
    }, 300);

    return () => clearTimeout(timeoutId);
  }, [localSearchQuery, searchQuery, searchNotes]);

  const handleCreateNote = async (e: React.FormEvent) => {
    e.preventDefault();
    const title = newNoteTitle.trim();
    
    if (!title) return;
    
    const newNote = await createNote({
      title,
      content: '',
      tags: []
    });
    
    if (newNote) {
      setNewNoteTitle('');
      setShowCreateForm(false);
      onNoteSelect(newNote);
    }
  };

  const handleDeleteNote = async (noteId: string, e: React.MouseEvent) => {
    e.stopPropagation(); // Prevent note selection
    
    if (window.confirm('Are you sure you want to delete this note?')) {
      await deleteNote(noteId);
    }
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

  const getPreviewText = (content: string) => {
    // Remove HTML tags and get first 100 characters
    const textContent = content.replace(/<[^>]*>/g, '');
    return textContent.length > 100 
      ? textContent.substring(0, 100) + '...' 
      : textContent;
  };

  const sortedNotes = [...notes].sort((a, b) => {
    const { sortBy = 'updatedAt', sortOrder = 'desc' } = filters;
    
    let aValue: any, bValue: any;
    
    switch (sortBy) {
      case 'title':
        aValue = a.title.toLowerCase();
        bValue = b.title.toLowerCase();
        break;
      case 'createdAt':
        aValue = new Date(a.createdAt);
        bValue = new Date(b.createdAt);
        break;
      case 'updatedAt':
      default:
        aValue = new Date(a.updatedAt);
        bValue = new Date(b.updatedAt);
        break;
    }
    
    if (sortOrder === 'desc') {
      return aValue > bValue ? -1 : aValue < bValue ? 1 : 0;
    } else {
      return aValue < bValue ? -1 : aValue > bValue ? 1 : 0;
    }
  });

  return (
    <div className="flex flex-col h-full bg-white border-r border-gray-200">
      {/* Header */}
      <div className="p-4 border-b border-gray-200">
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-lg font-semibold text-gray-900">Notes</h2>
          <button
            onClick={() => setShowCreateForm(true)}
            className="p-2 text-blue-600 hover:bg-blue-50 rounded-full transition-colors"
            title="Create new note"
          >
            <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
            </svg>
          </button>
        </div>

        {/* Search bar */}
        <div className="relative">
          <input
            type="text"
            placeholder="Search notes..."
            value={localSearchQuery}
            onChange={(e) => setLocalSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <svg className="h-4 w-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>
          {localSearchQuery && (
            <button
              onClick={() => setLocalSearchQuery('')}
              className="absolute inset-y-0 right-0 pr-3 flex items-center"
            >
              <svg className="h-4 w-4 text-gray-400 hover:text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          )}
        </div>

        {/* Filters */}
        <div className="mt-3 flex items-center justify-between text-sm">
          <div className="flex items-center space-x-2">
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
          
          <div className="text-gray-500">
            {notes.length} {notes.length === 1 ? 'note' : 'notes'}
          </div>
        </div>
      </div>

      {/* Create note form */}
      {showCreateForm && (
        <div className="p-4 border-b border-gray-200 bg-gray-50">
          <form onSubmit={handleCreateNote}>
            <input
              type="text"
              placeholder="Enter note title..."
              value={newNoteTitle}
              onChange={(e) => setNewNoteTitle(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent mb-3"
              autoFocus
              maxLength={200}
            />
            <div className="flex space-x-2">
              <button
                type="submit"
                disabled={!newNoteTitle.trim()}
                className="px-3 py-1 bg-blue-600 text-white rounded text-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Create
              </button>
              <button
                type="button"
                onClick={() => {
                  setShowCreateForm(false);
                  setNewNoteTitle('');
                }}
                className="px-3 py-1 bg-gray-300 text-gray-700 rounded text-sm hover:bg-gray-400 focus:outline-none"
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Error display */}
      {error && (
        <div className="p-4 border-b border-gray-200">
          <div className="flex items-center justify-between p-3 bg-red-50 border border-red-200 rounded-lg">
            <div className="flex">
              <svg className="h-4 w-4 text-red-400 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <p className="text-sm text-red-700">{error}</p>
            </div>
            <button
              onClick={clearError}
              className="text-red-400 hover:text-red-600"
            >
              <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>
      )}

      {/* Notes list */}
      <div className="flex-1 overflow-y-auto">
        {isLoading ? (
          <div className="p-4 text-center text-gray-500">
            <div className="animate-spin h-6 w-6 border-2 border-blue-600 rounded-full border-t-transparent mx-auto mb-2"></div>
            Loading notes...
          </div>
        ) : sortedNotes.length === 0 ? (
          <div className="p-4 text-center text-gray-500">
            {searchQuery ? (
              <div>
                <svg className="mx-auto h-8 w-8 text-gray-300 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                <p>No notes found for "{searchQuery}"</p>
              </div>
            ) : (
              <div>
                <svg className="mx-auto h-8 w-8 text-gray-300 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                <p>No notes yet</p>
                <p className="text-sm mt-1">Create your first note to get started</p>
              </div>
            )}
          </div>
        ) : (
          <div className="divide-y divide-gray-100">
            {sortedNotes.map((note) => (
              <div
                key={note.id}
                onClick={() => onNoteSelect(note)}
                className={`p-4 cursor-pointer hover:bg-gray-50 transition-colors ${
                  selectedNoteId === note.id ? 'bg-blue-50 border-r-2 border-blue-600' : ''
                }`}
              >
                <div className="flex items-start justify-between">
                  <div className="flex-1 min-w-0">
                    <h3 className="text-sm font-medium text-gray-900 truncate">
                      {note.title}
                    </h3>
                    <p className="text-xs text-gray-500 mt-1">
                      {formatDate(note.updatedAt)}
                    </p>
                    {note.content && (
                      <p className="text-xs text-gray-600 mt-2 line-clamp-2">
                        {getPreviewText(note.content)}
                      </p>
                    )}
                    {note.tags && note.tags.length > 0 && (
                      <div className="mt-2 flex flex-wrap gap-1">
                        {note.tags.slice(0, 3).map((tag) => (
                          <span
                            key={tag}
                            className="inline-flex items-center px-1.5 py-0.5 rounded text-xs font-medium bg-blue-100 text-blue-700"
                          >
                            #{tag}
                          </span>
                        ))}
                        {note.tags.length > 3 && (
                          <span className="text-xs text-gray-400">
                            +{note.tags.length - 3} more
                          </span>
                        )}
                      </div>
                    )}
                  </div>
                  
                  <button
                    onClick={(e) => handleDeleteNote(note.id, e)}
                    className="ml-2 p-1 text-gray-400 hover:text-red-600 rounded transition-colors opacity-0 group-hover:opacity-100"
                    title="Delete note"
                  >
                    <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};