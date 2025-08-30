// NoteMetadata component - displays note information and title editing
// Shows creation date, tags, and allows inline title editing

import React, {useEffect, useRef, useState} from 'react';
import {Note} from '../types/note';
import {useNoteStore} from '../stores/noteStore';

interface NoteMetadataProps {
  note: Note;
}

export const NoteMetadata: React.FC<NoteMetadataProps> = ({ note }) => {
  const { updateNote } = useNoteStore();
  const [isEditingTitle, setIsEditingTitle] = useState(false);
  const [title, setTitle] = useState(note.title);
  const titleInputRef = useRef<HTMLInputElement>(null);

  // Reset title when note changes
  useEffect(() => {
    setTitle(note.title);
  }, [note.title]);

  // Focus input when editing starts
  useEffect(() => {
    if (isEditingTitle && titleInputRef.current) {
      titleInputRef.current.focus();
      titleInputRef.current.select();
    }
  }, [isEditingTitle]);

  const handleTitleSave = async () => {
    const trimmedTitle = title.trim();
    
    if (trimmedTitle && trimmedTitle !== note.title) {
      try {
        await updateNote(note.id, { title: trimmedTitle });
      } catch (error) {
        // Revert title on error
        setTitle(note.title);
      }
    } else {
      // Revert to original title if empty or unchanged
      setTitle(note.title);
    }
    
    setIsEditingTitle(false);
  };

  const handleTitleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleTitleSave();
    } else if (e.key === 'Escape') {
      setTitle(note.title);
      setIsEditingTitle(false);
    }
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffInMs = now.getTime() - date.getTime();
    const diffInDays = Math.floor(diffInMs / (1000 * 60 * 60 * 24));
    
    if (diffInDays === 0) {
      return `Today at ${date.toLocaleTimeString([], { 
        hour: '2-digit', 
        minute: '2-digit' 
      })}`;
    } else if (diffInDays === 1) {
      return `Yesterday at ${date.toLocaleTimeString([], { 
        hour: '2-digit', 
        minute: '2-digit' 
      })}`;
    } else if (diffInDays < 7) {
      return `${diffInDays} days ago`;
    } else {
      return date.toLocaleDateString([], {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
      });
    }
  };

  return (
    <div className="border-b bg-white px-6 py-4">
      <div className="flex items-start justify-between">
        <div className="flex-1 min-w-0">
          {/* Title */}
          {isEditingTitle ? (
            <input
              ref={titleInputRef}
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              onBlur={handleTitleSave}
              onKeyDown={handleTitleKeyDown}
              className="text-2xl font-bold text-gray-900 bg-transparent border-none outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 rounded px-1 w-full"
              maxLength={200}
            />
          ) : (
            <h1
              onClick={() => setIsEditingTitle(true)}
              className="text-2xl font-bold text-gray-900 cursor-pointer hover:text-gray-700 transition-colors truncate"
              title="Click to edit title"
            >
              {note.title}
            </h1>
          )}

          {/* Metadata row */}
          <div className="mt-2 flex items-center text-sm text-gray-500 space-x-4">
            {/* Created date */}
            <div className="flex items-center">
              <svg className="h-4 w-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
              Created {formatDate(note.createdAt)}
            </div>

            {/* Updated date (if different from created) */}
            {note.updatedAt !== note.createdAt && (
              <div className="flex items-center">
                <svg className="h-4 w-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                </svg>
                Updated {formatDate(note.updatedAt)}
              </div>
            )}

            {/* Author */}
            <div className="flex items-center">
              <svg className="h-4 w-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
              {note.createdBy.displayName}
            </div>

            {/* Note ID for debugging/reference */}
            <div 
              className="font-mono text-xs opacity-50 cursor-pointer"
              title="Note ID (click to copy)"
              onClick={() => {
                navigator.clipboard.writeText(note.id);
              }}
            >
              #{note.id.slice(-8)}
            </div>
          </div>

          {/* Tags */}
          {note.tags && note.tags.length > 0 && (
            <div className="mt-3 flex flex-wrap gap-2">
              {note.tags.map((tag) => (
                <span
                  key={tag}
                  className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800"
                >
                  #{tag}
                </span>
              ))}
            </div>
          )}
        </div>

        {/* Actions */}
        <div className="flex items-center space-x-2 ml-4">
          <button
            title="Note actions"
            className="p-2 text-gray-400 hover:text-gray-600 rounded-full hover:bg-gray-100 transition-colors"
          >
            <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2z" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  );
};