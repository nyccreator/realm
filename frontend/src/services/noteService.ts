// Note service for Section 3.2 - Rich Text Editing & Manual Linking
// Handles all API calls to the backend note management endpoints

import {
    ApiError,
    CreateNoteLinkRequest,
    CreateNoteRequest,
    Note,
    NoteLink,
    SearchNotesRequest,
    SearchNotesResponse,
    UpdateNoteRequest
} from '../types/note';
import AuthService from './authService';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
const NOTES_BASE_URL = `${API_BASE_URL}/notes`;

class NoteService {
  private static instance: NoteService;
  private authService = AuthService.getInstance();

  public static getInstance(): NoteService {
    if (!NoteService.instance) {
      NoteService.instance = new NoteService();
    }
    return NoteService.instance;
  }

  /**
   * Normalize ID to string for API calls
   */
  private normalizeId(id: string | number): string {
    return String(id);
  }

  /**
   * Get authorization headers for API requests
   */
  private getAuthHeaders(): Record<string, string> {
    const token = this.authService.getStoredAccessToken();
    return {
      'Content-Type': 'application/json',
      ...(token && { 'Authorization': `Bearer ${token}` })
    };
  }

  /**
   * Handle API response errors
   */
  private async handleResponse<T>(response: Response): Promise<T> {
    const data = await response.json();
    
    if (!response.ok) {
      const error: ApiError = {
        message: data.message || 'Request failed',
        status: response.status,
        details: data.details || []
      };
      throw error;
    }

    return data as T;
  }

  /**
   * Create a new note
   */
  async createNote(noteData: CreateNoteRequest): Promise<Note> {
    const response = await fetch(NOTES_BASE_URL, {
      method: 'POST',
      headers: this.getAuthHeaders(),
      body: JSON.stringify(noteData),
    });

    return this.handleResponse<Note>(response);
  }

  /**
   * Get all user notes
   */
  async getNotes(): Promise<Note[]> {
    const response = await fetch(NOTES_BASE_URL, {
      method: 'GET',
      headers: this.getAuthHeaders(),
    });

    return this.handleResponse<Note[]>(response);
  }

  /**
   * Get a specific note by ID
   */
  async getNote(noteId: string | number): Promise<Note> {
    const response = await fetch(`${NOTES_BASE_URL}/${this.normalizeId(noteId)}`, {
      method: 'GET',
      headers: this.getAuthHeaders(),
    });

    return this.handleResponse<Note>(response);
  }

  /**
   * Update an existing note
   */
  async updateNote(noteId: string | number, updateData: UpdateNoteRequest): Promise<Note> {
    const response = await fetch(`${NOTES_BASE_URL}/${this.normalizeId(noteId)}`, {
      method: 'PUT',
      headers: this.getAuthHeaders(),
      body: JSON.stringify(updateData),
    });

    return this.handleResponse<Note>(response);
  }

  /**
   * Delete a note
   */
  async deleteNote(noteId: string | number): Promise<void> {
    const response = await fetch(`${NOTES_BASE_URL}/${this.normalizeId(noteId)}`, {
      method: 'DELETE',
      headers: this.getAuthHeaders(),
    });

    if (!response.ok) {
      const data = await response.json();
      throw new Error(data.message || 'Failed to delete note');
    }
  }

  /**
   * Create a link between two notes
   */
  async createLink(sourceNoteId: string | number, linkData: CreateNoteLinkRequest): Promise<NoteLink> {
    const response = await fetch(`${NOTES_BASE_URL}/${this.normalizeId(sourceNoteId)}/links`, {
      method: 'POST',
      headers: this.getAuthHeaders(),
      body: JSON.stringify(linkData),
    });

    return this.handleResponse<NoteLink>(response);
  }

  /**
   * Remove a link between notes
   */
  async removeLink(linkId: string | number): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/links/${this.normalizeId(linkId)}`, {
      method: 'DELETE',
      headers: this.getAuthHeaders(),
    });

    if (!response.ok) {
      const data = await response.json();
      throw new Error(data.message || 'Failed to remove link');
    }
  }

  /**
   * Get backlinks for a note (notes that link to this note)
   */
  async getBacklinks(noteId: string | number): Promise<Note[]> {
    const response = await fetch(`${NOTES_BASE_URL}/${this.normalizeId(noteId)}/backlinks`, {
      method: 'GET',
      headers: this.getAuthHeaders(),
    });

    return this.handleResponse<Note[]>(response);
  }

  /**
   * Search notes by query and filters
   */
  async searchNotes(searchParams: SearchNotesRequest = {}): Promise<SearchNotesResponse> {
    const queryParams = new URLSearchParams();
    
    // Backend requires a query parameter, so provide default if empty
    queryParams.append('query', searchParams.query || '');
    
    if (searchParams.tags && searchParams.tags.length > 0) {
      searchParams.tags.forEach(tag => queryParams.append('tag', tag));
    }
    
    if (searchParams.limit) {
      queryParams.append('limit', searchParams.limit.toString());
    }
    
    if (searchParams.offset) {
      queryParams.append('offset', searchParams.offset.toString());
    }

    const response = await fetch(`${NOTES_BASE_URL}/search?${queryParams}`, {
      method: 'GET',
      headers: this.getAuthHeaders(),
    });

    // Handle the response as a simple array of notes for now
    const notes = await this.handleResponse<Note[]>(response);
    return {
      notes,
      totalCount: notes.length,
      hasMore: false
    };
  }

  /**
   * Get all tags used in notes
   */
  async getTags(): Promise<string[]> {
    const response = await fetch(`${API_BASE_URL}/tags`, {
      method: 'GET',
      headers: this.getAuthHeaders(),
    });

    return this.handleResponse<string[]>(response);
  }

  /**
   * Auto-save note changes (used with debouncing)
   */
  async autoSaveNote(noteId: string | number, updateData: UpdateNoteRequest): Promise<Note> {
    // Use the same update endpoint but could add special handling for auto-save
    return this.updateNote(noteId, updateData);
  }

  /**
   * Bulk operations for multiple notes
   */
  async bulkDeleteNotes(noteIds: (string | number)[]): Promise<void> {
    const response = await fetch(`${NOTES_BASE_URL}/bulk/delete`, {
      method: 'POST',
      headers: this.getAuthHeaders(),
      body: JSON.stringify({ noteIds: noteIds.map(id => this.normalizeId(id)) }),
    });

    if (!response.ok) {
      const data = await response.json();
      throw new Error(data.message || 'Failed to delete notes');
    }
  }

  /**
   * Get note statistics (useful for dashboard)
   */
  async getNoteStats(): Promise<{
    totalNotes: number;
    totalTags: number;
    totalLinks: number;
    recentNotes: Note[];
  }> {
    const response = await fetch(`${NOTES_BASE_URL}/stats`, {
      method: 'GET',
      headers: this.getAuthHeaders(),
    });

    return this.handleResponse<{
      totalNotes: number;
      totalTags: number;
      totalLinks: number;
      recentNotes: Note[];
    }>(response);
  }
}

export default NoteService;