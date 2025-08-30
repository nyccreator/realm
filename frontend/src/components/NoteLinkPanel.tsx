// NoteLinkPanel component - manages note links and backlinks
// Shows outgoing links, backlinks, and provides interface for creating new links

import React, {useCallback, useEffect, useState} from 'react';
import {Note} from '../types/note';
import {useNoteStore} from '../stores/noteStore';
import {LinkCreationModal} from './LinkCreationModal';
import NoteService from '../services/noteService';

interface NoteLinkPanelProps {
  note: Note;
}

export const NoteLinkPanel: React.FC<NoteLinkPanelProps> = ({ note }) => {
  const { createLink, removeLink } = useNoteStore();
  const [showLinkModal, setShowLinkModal] = useState(false);
  const [backlinks, setBacklinks] = useState<Note[]>([]);
  const [loadingBacklinks, setLoadingBacklinks] = useState(false);

  const noteService = NoteService.getInstance();

  const loadBacklinks = useCallback(async () => {
    setLoadingBacklinks(true);
    try {
      const backlinkNotes = await noteService.getBacklinks(note.id);
      setBacklinks(backlinkNotes);
    } catch (error) {
      console.error('Failed to load backlinks:', error);
    } finally {
      setLoadingBacklinks(false);
    }
  }, [note.id, noteService]);

  // Load backlinks when note changes
  useEffect(() => {
    loadBacklinks();
  }, [loadBacklinks]);

  const handleCreateLink = async (targetNoteId: string, context?: string) => {
    const newLink = await createLink(note.id, {
      targetNoteId,
      context,
      type: 'REFERENCES'
    });

    if (newLink) {
      setShowLinkModal(false);
      // Refresh backlinks of the target note if it's currently open
      await loadBacklinks();
    }
  };

  const handleRemoveLink = async (linkId: string) => {
    if (window.confirm('Remove this link?')) {
      await removeLink(linkId);
      await loadBacklinks();
    }
  };

  const formatLinkPreview = (content: string) => {
    const textContent = content.replace(/<[^>]*>/g, '');
    return textContent.length > 150 
      ? textContent.substring(0, 150) + '...' 
      : textContent;
  };

  return (
    <div className="bg-white border rounded-lg">
      {/* Header */}
      <div className="px-4 py-3 border-b border-gray-200">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-medium text-gray-900">Links & References</h3>
          <button
            onClick={() => setShowLinkModal(true)}
            className="inline-flex items-center px-3 py-1.5 border border-transparent text-sm font-medium rounded-md text-blue-700 bg-blue-100 hover:bg-blue-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
          >
            <svg className="h-4 w-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
            </svg>
            Link Note
          </button>
        </div>
      </div>

      <div className="divide-y divide-gray-200">
        {/* Outgoing Links */}
        <div className="p-4">
          <h4 className="text-sm font-medium text-gray-900 mb-3 flex items-center">
            <svg className="h-4 w-4 mr-2 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
            </svg>
            Outgoing Links ({note.outgoingLinks?.length || 0})
          </h4>

          {!note.outgoingLinks || note.outgoingLinks.length === 0 ? (
            <p className="text-sm text-gray-500 italic">
              No outgoing links. Create links to connect this note to other notes.
            </p>
          ) : (
            <div className="space-y-3">
              {note.outgoingLinks.map((link) => (
                <div key={link.id} className="border border-gray-200 rounded-lg p-3 hover:bg-gray-50">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <div className="flex items-center">
                        <h5 className="text-sm font-medium text-gray-900 hover:text-blue-600 cursor-pointer">
                          {link.targetNote?.title || 'Unknown Note'}
                        </h5>
                        {link.type && link.type !== 'REFERENCES' && (
                          <span className="ml-2 inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-800">
                            {link.type}
                          </span>
                        )}
                      </div>
                      
                      {link.context && (
                        <p className="mt-1 text-xs text-gray-600 italic">
                          "{link.context}"
                        </p>
                      )}
                      
                      {link.targetNote?.content && (
                        <p className="mt-2 text-xs text-gray-500">
                          {formatLinkPreview(link.targetNote.content)}
                        </p>
                      )}
                      
                      <p className="mt-2 text-xs text-gray-400">
                        Linked {new Date(link.createdAt).toLocaleDateString()}
                      </p>
                    </div>
                    
                    <button
                      onClick={() => handleRemoveLink(link.id)}
                      className="ml-2 p-1 text-gray-400 hover:text-red-600 rounded transition-colors"
                      title="Remove link"
                    >
                      <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                      </svg>
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Backlinks */}
        <div className="p-4">
          <h4 className="text-sm font-medium text-gray-900 mb-3 flex items-center">
            <svg className="h-4 w-4 mr-2 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6" />
            </svg>
            Backlinks ({backlinks.length})
          </h4>

          {loadingBacklinks ? (
            <div className="text-center py-4">
              <div className="animate-spin h-4 w-4 border-2 border-gray-300 rounded-full border-t-blue-600 mx-auto"></div>
              <p className="text-sm text-gray-500 mt-2">Loading backlinks...</p>
            </div>
          ) : backlinks.length === 0 ? (
            <p className="text-sm text-gray-500 italic">
              No notes link to this note yet.
            </p>
          ) : (
            <div className="space-y-3">
              {backlinks.map((backlinkNote) => (
                <div key={backlinkNote.id} className="border border-gray-200 rounded-lg p-3 hover:bg-gray-50">
                  <div className="flex items-start">
                    <div className="flex-1">
                      <h5 className="text-sm font-medium text-gray-900 hover:text-blue-600 cursor-pointer">
                        {backlinkNote.title}
                      </h5>
                      
                      {backlinkNote.content && (
                        <p className="mt-2 text-xs text-gray-500">
                          {formatLinkPreview(backlinkNote.content)}
                        </p>
                      )}
                      
                      <div className="mt-2 flex items-center text-xs text-gray-400">
                        <span>Updated {new Date(backlinkNote.updatedAt).toLocaleDateString()}</span>
                        {backlinkNote.tags && backlinkNote.tags.length > 0 && (
                          <>
                            <span className="mx-2">•</span>
                            <div className="flex flex-wrap gap-1">
                              {backlinkNote.tags.slice(0, 2).map((tag) => (
                                <span key={tag} className="inline-flex items-center px-1.5 py-0.5 rounded text-xs bg-blue-100 text-blue-700">
                                  #{tag}
                                </span>
                              ))}
                              {backlinkNote.tags.length > 2 && (
                                <span>+{backlinkNote.tags.length - 2} more</span>
                              )}
                            </div>
                          </>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Link Statistics */}
        <div className="px-4 py-3 bg-gray-50">
          <div className="flex justify-between text-sm text-gray-600">
            <span>Total connections: {(note.outgoingLinks?.length || 0) + backlinks.length}</span>
            <span>Network strength: {Math.min(((note.outgoingLinks?.length || 0) + backlinks.length) * 10, 100)}%</span>
          </div>
        </div>
      </div>

      {/* Link Creation Modal */}
      {showLinkModal && (
        <LinkCreationModal
          sourceNote={note}
          onCreateLink={handleCreateLink}
          onClose={() => setShowLinkModal(false)}
        />
      )}
    </div>
  );
};