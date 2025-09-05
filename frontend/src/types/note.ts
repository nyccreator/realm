// Note Management Types for Section 3.2
// TypeScript interfaces for notes, links, and tags

export interface Note {
  id: string | number; // Handle both string and Long from backend
  title: string;
  content: string; // Rich text JSON from TipTap editor
  tags: string[];
  createdAt: string; // ISO string
  updatedAt: string; // ISO string
  createdBy: {
    id: string | number; // Handle both string and Long from backend
    displayName: string;
    email: string;
  };
  outgoingLinks?: NoteLink[];
  backlinks?: NoteLink[];
}

export interface NoteLink {
  id: string | number; // Handle both string and Long from backend
  sourceNoteId: string | number;
  targetNoteId: string | number;
  context?: string; // Optional context/description for the link
  type?: string; // Link type (default: 'references')
  createdAt: string;
  createdBy: {
    id: string | number; // Handle both string and Long from backend
    displayName: string;
  };
  targetNote?: Note; // Populated for outgoing links
  sourceNote?: Note; // Populated for backlinks
}

export interface CreateNoteRequest {
  title: string;
  content: string;
  tags?: string[];
}

export interface UpdateNoteRequest {
  title?: string;
  content?: string;
  tags?: string[];
}

export interface CreateNoteLinkRequest {
  targetNoteId: string | number;
  context?: string;
  type?: string;
}

export interface SearchNotesRequest {
  query?: string;
  tags?: string[];
  limit?: number;
  offset?: number;
}

export interface SearchNotesResponse {
  notes: Note[];
  totalCount: number;
  hasMore: boolean;
}

export interface NoteFilters {
  tags?: string[];
  searchQuery?: string;
  sortBy?: 'title' | 'createdAt' | 'updatedAt';
  sortOrder?: 'asc' | 'desc';
}

export interface Tag {
  name: string;
  count: number;
  color?: string;
}

// API Response types
export interface ApiResponse<T> {
  data: T;
  message?: string;
  status: number;
}

export interface ApiError {
  message: string;
  status: number;
  details?: string[];
}

// Editor-related types for TipTap integration
export interface EditorState {
  isEditing: boolean;
  hasUnsavedChanges: boolean;
  isSaving: boolean;
  lastSavedAt?: Date;
}

export interface AutoSaveOptions {
  debounceMs: number;
  enabled: boolean;
}