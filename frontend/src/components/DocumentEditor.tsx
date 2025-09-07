// Document Editor with Craft-inspired clean aesthetic
// Focused writing experience with minimal UI

import React, {useEffect, useMemo} from 'react';
import {EditorContent, useEditor} from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import Link from '@tiptap/extension-link';
import Placeholder from '@tiptap/extension-placeholder';
import {motion} from 'framer-motion';
import {Note} from '../types/note';
import {useNoteStore} from '../stores/noteStore';

interface DocumentEditorProps {
  note: Note;
  className?: string;
}

export const DocumentEditor: React.FC<DocumentEditorProps> = ({
  note,
  className = ''
}) => {
  const {
    updateEditorState,
    editorState,
    autoSaveCurrentNote,
    clearError
  } = useNoteStore();

  // Debounced auto-save function
  const debouncedAutoSave = useMemo(() => {
    let timeoutId: NodeJS.Timeout;
    return () => {
      clearTimeout(timeoutId);
      timeoutId = setTimeout(() => {
        if (editorState.hasUnsavedChanges && note) {
          autoSaveCurrentNote();
        }
      }, 1000);
    };
  }, [editorState.hasUnsavedChanges, note, autoSaveCurrentNote]);

  // TipTap editor configuration with Craft-inspired styling
  const editor = useEditor({
    extensions: [
      StarterKit.configure({
        heading: {
          levels: [1, 2, 3],
          HTMLAttributes: {
            class: 'craft-heading'
          }
        },
        paragraph: {
          HTMLAttributes: {
            class: 'craft-paragraph'
          }
        },
        bulletList: {
          keepMarks: true,
          keepAttributes: false,
          HTMLAttributes: {
            class: 'craft-bullet-list'
          }
        },
        orderedList: {
          keepMarks: true,
          keepAttributes: false,
          HTMLAttributes: {
            class: 'craft-ordered-list'
          }
        },
        blockquote: {
          HTMLAttributes: {
            class: 'craft-blockquote'
          }
        },
        code: {
          HTMLAttributes: {
            class: 'craft-inline-code'
          }
        },
        codeBlock: {
          HTMLAttributes: {
            class: 'craft-code-block'
          }
        }
      }),
      Link.configure({
        openOnClick: false,
        HTMLAttributes: {
          class: 'craft-link'
        }
      }),
      Placeholder.configure({
        placeholder: 'Start writing...',
        emptyEditorClass: 'craft-placeholder'
      })
    ],
    content: note?.content || '',
    editable: true,
    autofocus: true,
    onUpdate: ({ editor }) => {
      const content = editor.getHTML();
      const updatedNote = { ...note, content };
      useNoteStore.setState({ currentNote: updatedNote });
      updateEditorState({ hasUnsavedChanges: true });
      clearError();
      debouncedAutoSave();
    },
    onFocus: () => {
      updateEditorState({ isEditing: true });
    },
    onBlur: () => {
      updateEditorState({ isEditing: false });
    }
  }, [note?.id]);

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
  }, [note?.id, note?.content, note, editor, updateEditorState]);

  return (
    <motion.div 
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className={`flex flex-col h-full bg-white ${className}`}
    >
      {/* Document Header */}
      <div className="flex-shrink-0 px-8 pt-8 pb-4 border-b border-gray-100">
        <div className="max-w-4xl mx-auto">
          <input
            type="text"
            value={note.title || ''}
            onChange={(e) => {
              const updatedNote = { ...note, title: e.target.value };
              useNoteStore.setState({ currentNote: updatedNote });
              updateEditorState({ hasUnsavedChanges: true });
              debouncedAutoSave();
            }}
            placeholder="Untitled"
            className="
              w-full text-3xl font-bold text-gray-900 bg-transparent
              border-none outline-none resize-none
              placeholder-gray-400
              craft-title-input
            "
          />
          
          {/* Document Metadata */}
          <div className="flex items-center mt-4 text-sm text-gray-500 space-x-4">
            <span>
              {new Date(note.updatedAt).toLocaleDateString('en-US', {
                year: 'numeric',
                month: 'long',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
              })}
            </span>
            
            {editorState.hasUnsavedChanges && (
              <span className="flex items-center text-amber-600">
                <div className="w-2 h-2 bg-amber-500 rounded-full mr-2"></div>
                Unsaved changes
              </span>
            )}
            
            {editorState.isSaving && (
              <span className="flex items-center text-blue-600">
                <div className="w-2 h-2 bg-blue-500 rounded-full mr-2 animate-pulse"></div>
                Saving...
              </span>
            )}
            
            {editorState.lastSavedAt && !editorState.hasUnsavedChanges && !editorState.isSaving && (
              <span className="flex items-center text-green-600">
                <div className="w-2 h-2 bg-green-500 rounded-full mr-2"></div>
                Saved
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Editor Content */}
      <div className="flex-1 overflow-y-auto">
        <div className="max-w-4xl mx-auto px-8 py-8">
          <EditorContent 
            editor={editor}
            className="craft-editor-content"
          />
        </div>
      </div>

    </motion.div>
  );
};