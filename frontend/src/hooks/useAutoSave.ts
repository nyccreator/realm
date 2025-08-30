// useAutoSave hook - manages auto-save functionality with debouncing
// Provides configurable auto-save behavior for note editing

import {useCallback, useEffect, useRef} from 'react';
import {useNoteStore} from '../stores/noteStore';
import {AutoSaveOptions, Note} from '../types/note';

interface UseAutoSaveProps {
  note: Note | null;
  content: string;
  options?: Partial<AutoSaveOptions>;
}

interface UseAutoSaveReturn {
  isSaving: boolean;
  lastSavedAt: Date | undefined;
  hasUnsavedChanges: boolean;
  save: () => Promise<void>;
  enableAutoSave: () => void;
  disableAutoSave: () => void;
}

const DEFAULT_OPTIONS: AutoSaveOptions = {
  debounceMs: 1000,
  enabled: true
};

export const useAutoSave = ({ 
  note, 
  content,
  options = {} 
}: UseAutoSaveProps): UseAutoSaveReturn => {
  const {
    editorState,
    updateEditorState,
    autoSaveCurrentNote,
    updateNote
  } = useNoteStore();

  const mergedOptions = { ...DEFAULT_OPTIONS, ...options };
  const timeoutRef = useRef<NodeJS.Timeout | null>(null);
  const lastContentRef = useRef<string>('');
  const initialContentRef = useRef<string>('');

  // Initialize refs when note changes
  useEffect(() => {
    if (note) {
      lastContentRef.current = note.content;
      initialContentRef.current = note.content;
    }
  }, [note?.id]);

  // Track content changes and update unsaved state
  useEffect(() => {
    if (!note) return;

    const hasChanges = content !== lastContentRef.current;
    
    if (hasChanges !== editorState.hasUnsavedChanges) {
      updateEditorState({ hasUnsavedChanges: hasChanges });
    }
  }, [content, note, editorState.hasUnsavedChanges, updateEditorState]);

  // Auto-save function with debouncing
  const triggerAutoSave = useCallback(async () => {
    if (!note || !editorState.hasUnsavedChanges || !mergedOptions.enabled) {
      return;
    }

    try {
      updateEditorState({ isSaving: true });
      
      // Update the note with current content
      await updateNote(note.id, { 
        content,
        title: note.title,
        tags: note.tags
      });
      
      lastContentRef.current = content;
      
      updateEditorState({ 
        hasUnsavedChanges: false,
        isSaving: false,
        lastSavedAt: new Date()
      });
    } catch (error) {
      console.error('Auto-save failed:', error);
      updateEditorState({ isSaving: false });
    }
  }, [note, content, editorState.hasUnsavedChanges, mergedOptions.enabled, updateNote, updateEditorState]);

  // Debounced auto-save effect
  useEffect(() => {
    if (!note || !editorState.hasUnsavedChanges || !mergedOptions.enabled) {
      return;
    }

    // Clear existing timeout
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
    }

    // Set new timeout for auto-save
    timeoutRef.current = setTimeout(() => {
      triggerAutoSave();
    }, mergedOptions.debounceMs);

    // Cleanup timeout on unmount or dependency change
    return () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
    };
  }, [content, triggerAutoSave, mergedOptions.debounceMs, mergedOptions.enabled, note, editorState.hasUnsavedChanges]);

  // Manual save function
  const save = useCallback(async () => {
    if (!note) return;

    try {
      updateEditorState({ isSaving: true });
      
      await updateNote(note.id, { 
        content,
        title: note.title,
        tags: note.tags
      });
      
      lastContentRef.current = content;
      
      updateEditorState({ 
        hasUnsavedChanges: false,
        isSaving: false,
        lastSavedAt: new Date()
      });
    } catch (error) {
      console.error('Manual save failed:', error);
      updateEditorState({ isSaving: false });
      throw error;
    }
  }, [note, content, updateNote, updateEditorState]);

  // Enable/disable auto-save
  const enableAutoSave = useCallback(() => {
    mergedOptions.enabled = true;
  }, [mergedOptions]);

  const disableAutoSave = useCallback(() => {
    mergedOptions.enabled = false;
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
      timeoutRef.current = null;
    }
  }, [mergedOptions]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
    };
  }, []);

  // Save on browser beforeunload if there are unsaved changes
  useEffect(() => {
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      if (editorState.hasUnsavedChanges) {
        e.preventDefault();
        e.returnValue = 'You have unsaved changes. Are you sure you want to leave?';
        
        // Attempt to save immediately (non-blocking)
        if (note) {
          navigator.sendBeacon('/api/notes/' + note.id, JSON.stringify({
            content,
            title: note.title,
            tags: note.tags
          }));
        }
      }
    };

    window.addEventListener('beforeunload', handleBeforeUnload);
    
    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
    };
  }, [editorState.hasUnsavedChanges, note, content]);

  // Save on page visibility change (when tab becomes hidden)
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.hidden && editorState.hasUnsavedChanges && note) {
        // Immediate save when page becomes hidden
        triggerAutoSave();
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    
    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [editorState.hasUnsavedChanges, note, triggerAutoSave]);

  return {
    isSaving: editorState.isSaving,
    lastSavedAt: editorState.lastSavedAt,
    hasUnsavedChanges: editorState.hasUnsavedChanges,
    save,
    enableAutoSave,
    disableAutoSave
  };
};