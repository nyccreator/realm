// Zustand store for Section 3.2 - Rich Text Editing & Manual Linking
// Manages note state, UI state, and provides actions for note operations

import {create} from 'zustand';
import {devtools} from 'zustand/middleware';
import {
    CreateNoteLinkRequest,
    CreateNoteRequest,
    EditorState,
    Note,
    NoteFilters,
    NoteLink,
    UpdateNoteRequest
} from '../types/note';
import NoteService from '../services/noteService';

interface NoteState {
  // Data state
  notes: Note[];
  currentNote: Note | null;
  selectedNotes: Set<string>;
  tags: string[];
  
  // UI state
  isLoading: boolean;
  isSaving: boolean;
  error: string | null;
  searchQuery: string;
  filters: NoteFilters;
  
  // Editor state
  editorState: EditorState;
  
  // Sidebar state
  sidebarOpen: boolean;
  
  // Actions for data operations
  loadNotes: () => Promise<void>;
  createNote: (noteData: CreateNoteRequest) => Promise<Note | null>;
  updateNote: (noteId: string, updateData: UpdateNoteRequest) => Promise<Note | null>;
  deleteNote: (noteId: string) => Promise<void>;
  selectNote: (noteId: string) => Promise<void>;
  
  // Link operations
  createLink: (sourceNoteId: string, linkData: CreateNoteLinkRequest) => Promise<NoteLink | null>;
  removeLink: (linkId: string) => Promise<void>;
  
  // Search and filter operations
  searchNotes: (query: string) => Promise<void>;
  setFilters: (filters: Partial<NoteFilters>) => void;
  clearFilters: () => void;
  
  // Editor operations
  setCurrentNote: (note: Note | null) => void;
  updateEditorState: (updates: Partial<EditorState>) => void;
  autoSaveCurrentNote: () => Promise<void>;
  
  // UI operations
  setError: (error: string | null) => void;
  clearError: () => void;
  toggleSidebar: () => void;
  toggleNoteSelection: (noteId: string) => void;
  clearSelection: () => void;
  
  // Tag operations
  loadTags: () => Promise<void>;
  addTagToCurrentNote: (tag: string) => Promise<void>;
  removeTagFromCurrentNote: (tag: string) => Promise<void>;
}

const noteService = NoteService.getInstance();

export const useNoteStore = create<NoteState>()(
  devtools(
    (set, get) => ({
      // Initial state
      notes: [],
      currentNote: null,
      selectedNotes: new Set<string>(),
      tags: [],
      
      isLoading: false,
      isSaving: false,
      error: null,
      searchQuery: '',
      filters: {
        tags: [],
        sortBy: 'updatedAt',
        sortOrder: 'desc'
      },
      
      editorState: {
        isEditing: false,
        hasUnsavedChanges: false,
        isSaving: false,
        lastSavedAt: undefined
      },
      
      sidebarOpen: true,

      // Data operations
      loadNotes: async () => {
        set({ isLoading: true, error: null });
        try {
          const notes = await noteService.getNotes();
          set({ notes, isLoading: false });
        } catch (error: any) {
          set({ 
            error: error.message || 'Failed to load notes', 
            isLoading: false 
          });
        }
      },

      createNote: async (noteData: CreateNoteRequest) => {
        set({ isSaving: true, error: null });
        try {
          const newNote = await noteService.createNote(noteData);
          const { notes } = get();
          set({ 
            notes: [newNote, ...notes], 
            currentNote: newNote,
            isSaving: false 
          });
          return newNote;
        } catch (error: any) {
          set({ 
            error: error.message || 'Failed to create note', 
            isSaving: false 
          });
          return null;
        }
      },

      updateNote: async (noteId: string, updateData: UpdateNoteRequest) => {
        set({ isSaving: true, error: null });
        try {
          const updatedNote = await noteService.updateNote(noteId, updateData);
          const { notes, currentNote } = get();
          
          const updatedNotes = notes.map(note => 
            note.id === noteId ? updatedNote : note
          );
          
          set({ 
            notes: updatedNotes,
            currentNote: currentNote?.id === noteId ? updatedNote : currentNote,
            isSaving: false,
            editorState: {
              ...get().editorState,
              hasUnsavedChanges: false,
              lastSavedAt: new Date()
            }
          });
          return updatedNote;
        } catch (error: any) {
          set({ 
            error: error.message || 'Failed to update note', 
            isSaving: false 
          });
          return null;
        }
      },

      deleteNote: async (noteId: string) => {
        set({ isLoading: true, error: null });
        try {
          await noteService.deleteNote(noteId);
          const { notes, currentNote, selectedNotes } = get();
          
          const updatedNotes = notes.filter(note => note.id !== noteId);
          const newSelectedNotes = new Set(selectedNotes);
          newSelectedNotes.delete(noteId);
          
          set({ 
            notes: updatedNotes,
            currentNote: currentNote?.id === noteId ? null : currentNote,
            selectedNotes: newSelectedNotes,
            isLoading: false 
          });
        } catch (error: any) {
          set({ 
            error: error.message || 'Failed to delete note', 
            isLoading: false 
          });
        }
      },

      selectNote: async (noteId: string) => {
        set({ isLoading: true, error: null });
        try {
          const note = await noteService.getNote(noteId);
          set({ 
            currentNote: note, 
            isLoading: false,
            editorState: {
              isEditing: false,
              hasUnsavedChanges: false,
              isSaving: false,
              lastSavedAt: undefined
            }
          });
        } catch (error: any) {
          set({ 
            error: error.message || 'Failed to load note', 
            isLoading: false 
          });
        }
      },

      // Link operations
      createLink: async (sourceNoteId: string, linkData: CreateNoteLinkRequest) => {
        set({ error: null });
        try {
          const newLink = await noteService.createLink(sourceNoteId, linkData);
          
          // Update the current note if it's the source note
          const { currentNote } = get();
          if (currentNote?.id === sourceNoteId) {
            const updatedNote = {
              ...currentNote,
              outgoingLinks: [...(currentNote.outgoingLinks || []), newLink]
            };
            set({ currentNote: updatedNote });
          }
          
          return newLink;
        } catch (error: any) {
          set({ error: error.message || 'Failed to create link' });
          return null;
        }
      },

      removeLink: async (linkId: string) => {
        set({ error: null });
        try {
          await noteService.removeLink(linkId);
          
          // Update current note if it has this link
          const { currentNote } = get();
          if (currentNote?.outgoingLinks) {
            const updatedLinks = currentNote.outgoingLinks.filter(
              link => link.id !== linkId
            );
            set({ 
              currentNote: { 
                ...currentNote, 
                outgoingLinks: updatedLinks 
              } 
            });
          }
        } catch (error: any) {
          set({ error: error.message || 'Failed to remove link' });
        }
      },

      // Search and filter operations
      searchNotes: async (query: string) => {
        set({ searchQuery: query, isLoading: true, error: null });
        try {
          const searchResult = await noteService.searchNotes({ 
            query: query || undefined,
            tags: get().filters.tags?.length ? get().filters.tags : undefined
          });
          set({ 
            notes: searchResult.notes, 
            isLoading: false 
          });
        } catch (error: any) {
          set({ 
            error: error.message || 'Search failed', 
            isLoading: false 
          });
        }
      },

      setFilters: (filters: Partial<NoteFilters>) => {
        const currentFilters = get().filters;
        set({ 
          filters: { ...currentFilters, ...filters } 
        });
      },

      clearFilters: () => {
        set({ 
          filters: { 
            tags: [], 
            sortBy: 'updatedAt', 
            sortOrder: 'desc' 
          },
          searchQuery: ''
        });
      },

      // Editor operations
      setCurrentNote: (note: Note | null) => {
        set({ 
          currentNote: note,
          editorState: {
            isEditing: false,
            hasUnsavedChanges: false,
            isSaving: false,
            lastSavedAt: undefined
          }
        });
      },

      updateEditorState: (updates: Partial<EditorState>) => {
        const currentEditorState = get().editorState;
        set({ 
          editorState: { ...currentEditorState, ...updates } 
        });
      },

      autoSaveCurrentNote: async () => {
        const { currentNote, editorState } = get();
        
        if (!currentNote || !editorState.hasUnsavedChanges) {
          return;
        }

        set({ 
          editorState: { ...editorState, isSaving: true } 
        });

        try {
          await noteService.autoSaveNote(currentNote.id, {
            title: currentNote.title,
            content: currentNote.content,
            tags: currentNote.tags
          });
          
          set({ 
            editorState: { 
              ...get().editorState,
              hasUnsavedChanges: false,
              isSaving: false,
              lastSavedAt: new Date()
            } 
          });
        } catch (error: any) {
          set({ 
            editorState: { ...get().editorState, isSaving: false },
            error: 'Auto-save failed'
          });
        }
      },

      // UI operations
      setError: (error: string | null) => {
        set({ error });
      },

      clearError: () => {
        set({ error: null });
      },

      toggleSidebar: () => {
        set({ sidebarOpen: !get().sidebarOpen });
      },

      toggleNoteSelection: (noteId: string) => {
        const { selectedNotes } = get();
        const newSelection = new Set(selectedNotes);
        
        if (newSelection.has(noteId)) {
          newSelection.delete(noteId);
        } else {
          newSelection.add(noteId);
        }
        
        set({ selectedNotes: newSelection });
      },

      clearSelection: () => {
        set({ selectedNotes: new Set() });
      },

      // Tag operations
      loadTags: async () => {
        try {
          const tags = await noteService.getTags();
          set({ tags });
        } catch (error: any) {
          set({ error: error.message || 'Failed to load tags' });
        }
      },

      addTagToCurrentNote: async (tag: string) => {
        const { currentNote } = get();
        if (!currentNote || currentNote.tags.includes(tag)) {
          return;
        }

        const updatedTags = [...currentNote.tags, tag];
        try {
          const updatedNote = await noteService.updateNote(currentNote.id, {
            tags: updatedTags
          });
          set({ currentNote: updatedNote });
        } catch (error: any) {
          set({ error: error.message || 'Failed to add tag' });
        }
      },

      removeTagFromCurrentNote: async (tag: string) => {
        const { currentNote } = get();
        if (!currentNote || !currentNote.tags.includes(tag)) {
          return;
        }

        const updatedTags = currentNote.tags.filter(t => t !== tag);
        try {
          const updatedNote = await noteService.updateNote(currentNote.id, {
            tags: updatedTags
          });
          set({ currentNote: updatedNote });
        } catch (error: any) {
          set({ error: error.message || 'Failed to remove tag' });
        }
      },
    }),
    {
      name: 'note-store'
    }
  )
);