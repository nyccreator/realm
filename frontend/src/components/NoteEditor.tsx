// NoteEditor component for Section 3.2 - Rich Text Editing & Manual Linking
// TipTap-powered rich text editor with auto-save and linking capabilities

import React, {useCallback, useEffect, useMemo} from 'react';
import {EditorContent, useEditor} from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import Link from '@tiptap/extension-link';
import Placeholder from '@tiptap/extension-placeholder';
import {Note} from '../types/note';
import {useNoteStore} from '../stores/noteStore';
import {EditorToolbar} from './EditorToolbar';
import {NoteMetadata} from './NoteMetadata';

interface NoteEditorProps {
  note: Note | null;
  className?: string;
  autoFocus?: boolean;
}

export const NoteEditor: React.FC<NoteEditorProps> = ({ 
  note, 
  className = '',
  autoFocus = true 
}) => {
  const {
    updateNote,
    updateEditorState,
    editorState,
    autoSaveCurrentNote,
    setError,
    clearError
  } = useNoteStore();

  // Debounced auto-save function
  const debouncedAutoSave = useMemo(
    () => {
      let timeoutId: NodeJS.Timeout;
      return () => {
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => {
          if (editorState.hasUnsavedChanges && note) {
            autoSaveCurrentNote();
          }
        }, 1000); // 1 second debounce
      };
    },
    [editorState.hasUnsavedChanges, note, autoSaveCurrentNote]
  );

  // TipTap editor configuration
  const editor = useEditor({
    extensions: [
      StarterKit.configure({
        // Disable default heading levels we don't want
        heading: {
          levels: [1, 2, 3],
        },
        // Configure bullet list
        bulletList: {
          keepMarks: true,
          keepAttributes: false,
        },
        // Configure ordered list
        orderedList: {
          keepMarks: true,
          keepAttributes: false,
        },
      }),
      Link.configure({
        openOnClick: false,
        HTMLAttributes: {
          class: 'text-blue-600 hover:text-blue-800 underline cursor-pointer',
        },
      }),
      Placeholder.configure({
        placeholder: note ? 'Start writing your note...' : 'Select a note to start editing',
        emptyEditorClass: 'text-gray-400',
      }),
    ],
    content: note?.content || '',
    editable: !!note,
    autofocus: autoFocus && !!note,
    onUpdate: ({ editor }) => {
      if (!note) return;
      
      const content = editor.getHTML();
      
      // Update the current note content in the store
      const updatedNote = { ...note, content };
      useNoteStore.setState({ currentNote: updatedNote });
      
      // Mark as having unsaved changes
      updateEditorState({ hasUnsavedChanges: true });
      
      // Clear any previous errors
      clearError();
      
      // Trigger auto-save
      debouncedAutoSave();
    },
    onFocus: () => {
      updateEditorState({ isEditing: true });
    },
    onBlur: () => {
      updateEditorState({ isEditing: false });
    },
  }, [note?.id]); // Re-create editor when note changes

  // Update editor content when note changes
  useEffect(() => {
    if (editor && note) {
      const currentContent = editor.getHTML();
      if (currentContent !== note.content) {
        editor.commands.setContent(note.content || '');
        updateEditorState({ 
          hasUnsavedChanges: false,
          isEditing: false 
        });
      }
    }
  }, [note?.id, note?.content, editor, updateEditorState]);

  // Handle keyboard shortcuts
  const handleKeyDown = useCallback((event: KeyboardEvent) => {
    if (!note || !editor) return;

    // Ctrl/Cmd + S to save
    if ((event.ctrlKey || event.metaKey) && event.key === 's') {
      event.preventDefault();
      if (editorState.hasUnsavedChanges) {
        autoSaveCurrentNote();
      }
    }
    
    // Ctrl/Cmd + B for bold
    if ((event.ctrlKey || event.metaKey) && event.key === 'b') {
      event.preventDefault();
      editor.chain().focus().toggleBold().run();
    }
    
    // Ctrl/Cmd + I for italic
    if ((event.ctrlKey || event.metaKey) && event.key === 'i') {
      event.preventDefault();
      editor.chain().focus().toggleItalic().run();
    }
    
    // Ctrl/Cmd + K for link
    if ((event.ctrlKey || event.metaKey) && event.key === 'k') {
      event.preventDefault();
      const url = window.prompt('Enter URL:');
      if (url) {
        editor.chain().focus().setLink({ href: url }).run();
      }
    }
  }, [note, editor, editorState.hasUnsavedChanges, autoSaveCurrentNote]);

  // Attach keyboard event listeners
  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [handleKeyDown]);

  // Auto-save on unmount if there are unsaved changes
  useEffect(() => {
    return () => {
      if (editorState.hasUnsavedChanges && note) {
        autoSaveCurrentNote();
      }
    };
  }, [editorState.hasUnsavedChanges, note, autoSaveCurrentNote]);

  if (!note) {
    return (
      <div className={`flex-1 flex items-center justify-center ${className}`}>
        <div className="text-center text-gray-500">
          <div className="mb-4">
            <svg 
              className="mx-auto h-12 w-12 text-gray-300" 
              fill="none" 
              stroke="currentColor" 
              viewBox="0 0 24 24"
            >
              <path 
                strokeLinecap="round" 
                strokeLinejoin="round" 
                strokeWidth={2} 
                d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" 
              />
            </svg>
          </div>
          <h3 className="text-lg font-medium text-gray-900 mb-2">No note selected</h3>
          <p className="text-gray-500">
            Select a note from the sidebar to start editing, or create a new note.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className={`flex flex-col h-full ${className}`}>
      {/* Note metadata and status */}
      <NoteMetadata note={note} />
      
      {/* Editor toolbar */}
      <EditorToolbar editor={editor} />
      
      {/* Main editor area */}
      <div className="flex-1 flex flex-col overflow-hidden">
        <div className="flex-1 p-6 overflow-y-auto">
          <EditorContent 
            editor={editor}
            className="prose prose-lg max-w-none focus:outline-none"
          />
        </div>
        
        {/* Status bar */}
        <div className="border-t bg-gray-50 px-6 py-2 flex items-center justify-between text-sm text-gray-500">
          <div className="flex items-center space-x-4">
            {editorState.isSaving && (
              <div className="flex items-center">
                <div className="animate-spin h-3 w-3 border border-gray-300 rounded-full border-t-blue-600 mr-2"></div>
                Saving...
              </div>
            )}
            
            {editorState.lastSavedAt && !editorState.hasUnsavedChanges && (
              <div>
                Saved at {editorState.lastSavedAt.toLocaleTimeString()}
              </div>
            )}
            
            {editorState.hasUnsavedChanges && !editorState.isSaving && (
              <div className="text-orange-600">
                Unsaved changes
              </div>
            )}
          </div>
          
          <div className="flex items-center space-x-4">
            <span>
              {editor?.storage.characterCount?.characters() || 0} characters
            </span>
            <span>
              {editor?.storage.characterCount?.words() || 0} words
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};