// LinkCreationModal component - modal for creating links between notes
// Provides search interface to find target notes and add context

import React, {useEffect, useRef, useState} from 'react';
import {Note} from '../types/note';
import {useNoteStore} from '../stores/noteStore';

interface LinkCreationModalProps {
  sourceNote: Note;
  onCreateLink: (targetNoteId: string, context?: string) => Promise<void>;
  onClose: () => void;
}

export const LinkCreationModal: React.FC<LinkCreationModalProps> = ({
  sourceNote,
  onCreateLink,
  onClose
}) => {
  const { notes, searchNotes } = useNoteStore();
  const [searchQuery, setSearchQuery] = useState('');
  const [filteredNotes, setFilteredNotes] = useState<Note[]>([]);
  const [selectedNote, setSelectedNote] = useState<Note | null>(null);
  const [linkContext, setLinkContext] = useState('');
  const [isCreating, setIsCreating] = useState(false);
  
  const searchInputRef = useRef<HTMLInputElement>(null);
  const modalRef = useRef<HTMLDivElement>(null);

  // Focus search input when modal opens
  useEffect(() => {
    if (searchInputRef.current) {
      searchInputRef.current.focus();
    }
  }, []);

  // Filter notes based on search query
  useEffect(() => {
    if (!searchQuery.trim()) {
      // Show recent notes excluding the source note
      const recentNotes = notes
        .filter(note => note.id !== sourceNote.id)
        .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())
        .slice(0, 10);
      setFilteredNotes(recentNotes);
    } else {
      // Filter notes by search query
      const filtered = notes.filter(note => 
        note.id !== sourceNote.id &&
        (note.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
         note.content.toLowerCase().includes(searchQuery.toLowerCase()) ||
         note.tags.some(tag => tag.toLowerCase().includes(searchQuery.toLowerCase())))
      );
      setFilteredNotes(filtered);
    }
  }, [searchQuery, notes, sourceNote.id]);

  // Handle keyboard navigation
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Escape') {
      onClose();
    }
  };

  // Handle modal backdrop click
  const handleBackdropClick = (e: React.MouseEvent) => {
    if (e.target === modalRef.current) {
      onClose();
    }
  };

  const handleCreateLink = async () => {
    if (!selectedNote) return;

    setIsCreating(true);
    try {
      await onCreateLink(selectedNote.id, linkContext.trim() || undefined);
    } finally {
      setIsCreating(false);
    }
  };

  const getPreviewText = (content: string) => {
    const textContent = content.replace(/<[^>]*>/g, '');
    return textContent.length > 100 
      ? textContent.substring(0, 100) + '...' 
      : textContent;
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString([], {
      month: 'short',
      day: 'numeric',
      year: date.getFullYear() !== new Date().getFullYear() ? 'numeric' : undefined
    });
  };

  return (
    <div 
      ref={modalRef}
      className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50"
      onClick={handleBackdropClick}
      onKeyDown={handleKeyDown}
    >
      <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[80vh] flex flex-col">
        {/* Header */}
        <div className="px-6 py-4 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <h3 className="text-lg font-medium text-gray-900">
              Create Link from "{sourceNote.title}"
            </h3>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600 transition-colors"
            >
              <svg className="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>

        {/* Search */}
        <div className="px-6 py-4 border-b border-gray-200">
          <div className="relative">
            <input
              ref={searchInputRef}
              type="text"
              placeholder="Search for notes to link..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <svg className="h-5 w-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>
          </div>
        </div>

        {/* Notes List */}
        <div className="flex-1 overflow-y-auto">
          {filteredNotes.length === 0 ? (
            <div className="p-6 text-center text-gray-500">
              {searchQuery ? (
                <div>
                  <svg className="mx-auto h-8 w-8 text-gray-300 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                  <p>No notes found matching "{searchQuery}"</p>
                </div>
              ) : (
                <div>
                  <svg className="mx-auto h-8 w-8 text-gray-300 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                  <p>No other notes available to link</p>
                </div>
              )}
            </div>
          ) : (
            <div className="divide-y divide-gray-100">
              {filteredNotes.map((note) => (
                <div
                  key={note.id}
                  onClick={() => setSelectedNote(selectedNote?.id === note.id ? null : note)}
                  className={`p-4 cursor-pointer hover:bg-gray-50 transition-colors ${
                    selectedNote?.id === note.id ? 'bg-blue-50 border-l-4 border-blue-600' : ''
                  }`}
                >
                  <div className="flex items-start">
                    <div className="flex-1">
                      <div className="flex items-center">
                        <h4 className="text-sm font-medium text-gray-900">
                          {note.title}
                        </h4>
                        <span className="ml-2 text-xs text-gray-500">
                          {formatDate(note.updatedAt)}
                        </span>
                      </div>
                      
                      {note.content && (
                        <p className="mt-1 text-sm text-gray-600">
                          {getPreviewText(note.content)}
                        </p>
                      )}
                      
                      {note.tags && note.tags.length > 0 && (
                        <div className="mt-2 flex flex-wrap gap-1">
                          {note.tags.slice(0, 4).map((tag) => (
                            <span
                              key={tag}
                              className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-700"
                            >
                              #{tag}
                            </span>
                          ))}
                          {note.tags.length > 4 && (
                            <span className="text-xs text-gray-400">
                              +{note.tags.length - 4} more
                            </span>
                          )}
                        </div>
                      )}
                    </div>
                    
                    <div className="ml-4 flex items-center">
                      <div className={`w-4 h-4 rounded-full border-2 ${
                        selectedNote?.id === note.id 
                          ? 'border-blue-600 bg-blue-600' 
                          : 'border-gray-300'
                      }`}>
                        {selectedNote?.id === note.id && (
                          <div className="w-2 h-2 bg-white rounded-full m-0.5"></div>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Context Input and Actions */}
        {selectedNote && (
          <div className="px-6 py-4 border-t border-gray-200">
            <div className="mb-4">
              <label htmlFor="link-context" className="block text-sm font-medium text-gray-700 mb-2">
                Link context (optional)
              </label>
              <textarea
                id="link-context"
                rows={3}
                value={linkContext}
                onChange={(e) => setLinkContext(e.target.value)}
                placeholder="Describe the relationship between these notes..."
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
                maxLength={500}
              />
              <p className="mt-1 text-xs text-gray-500">
                {linkContext.length}/500 characters
              </p>
            </div>

            <div className="flex items-center justify-between">
              <div className="text-sm text-gray-600">
                Linking to: <span className="font-medium">{selectedNote.title}</span>
              </div>
              
              <div className="flex space-x-3">
                <button
                  onClick={onClose}
                  disabled={isCreating}
                  className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 border border-gray-300 rounded-md hover:bg-gray-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-gray-500 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Cancel
                </button>
                <button
                  onClick={handleCreateLink}
                  disabled={isCreating}
                  className="px-4 py-2 text-sm font-medium text-white bg-blue-600 border border-transparent rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
                >
                  {isCreating ? (
                    <>
                      <div className="animate-spin h-4 w-4 border border-white rounded-full border-t-transparent mr-2"></div>
                      Creating...
                    </>
                  ) : (
                    <>
                      <svg className="h-4 w-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
                      </svg>
                      Create Link
                    </>
                  )}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};