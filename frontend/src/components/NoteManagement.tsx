// NoteManagement component - main interface for Section 3.2
// Combines all note management functionality into a unified interface

import React, {useEffect, useState} from 'react';
import {useNoteStore} from '../stores/noteStore';
import {NoteList} from './NoteList';
import {NoteEditor} from './NoteEditor';
import {NoteLinkPanel} from './NoteLinkPanel';
import {TagManager} from './TagManager';
import {SearchInterface} from './SearchInterface';
import {GraphVisualization} from './GraphVisualization';
import {Note} from '../types/note';

export const NoteManagement: React.FC = () => {
  const {
    currentNote,
    sidebarOpen,
    error,
    isLoading,
    selectNote,
    setCurrentNote,
    toggleSidebar,
    clearError,
    loadNotes
  } = useNoteStore();

  const [activeTab, setActiveTab] = useState<'editor' | 'links' | 'tags' | 'graph'>('editor');

  // Load notes on mount
  useEffect(() => {
    loadNotes();
  }, [loadNotes]);

  const handleNoteSelect = async (note: Note) => {
    await selectNote(String(note.id));
    setActiveTab('editor'); // Switch to editor when selecting a note
  };

  return (
    <div className="flex h-screen bg-gray-100">
      {/* Mobile Overlay */}
      {sidebarOpen && (
        <div 
          className="fixed inset-0 bg-black bg-opacity-50 z-40 lg:hidden"
          onClick={toggleSidebar}
        />
      )}
      
      {/* Sidebar */}
      <div className={`
        ${sidebarOpen ? 'w-80' : 'w-0'} 
        lg:relative fixed inset-y-0 left-0 z-50
        transition-all duration-300 ease-in-out overflow-hidden 
        border-r border-gray-200 bg-white
        lg:w-80 lg:block
        ${sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}
      `}>
        {sidebarOpen && (
          <div className="h-full flex flex-col">
            {/* Search */}
            <div className="p-4 border-b border-gray-200">
              <SearchInterface
                onResultSelect={handleNoteSelect}
                placeholder="Search all notes..."
                showAdvanced={true}
              />
            </div>
            
            {/* Note List */}
            <div className="flex-1 overflow-hidden">
              <NoteList
                onNoteSelect={handleNoteSelect}
                selectedNoteId={currentNote?.id}
              />
            </div>
          </div>
        )}
      </div>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0 lg:ml-0">
        {/* Header */}
        <header className="bg-white border-b border-gray-200 px-4 py-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
              {/* Sidebar Toggle */}
              <button
                onClick={toggleSidebar}
                className="p-2 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
                title={sidebarOpen ? 'Hide sidebar' : 'Show sidebar'}
              >
                <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                </svg>
              </button>

              <div className="h-6 w-px bg-gray-300"></div>

              {/* Page Title */}
              <div>
                <h1 className="text-xl font-semibold text-gray-900">
                  {currentNote ? currentNote.title : 
                   activeTab === 'graph' ? 'Knowledge Graph' : 'Realm Notes'}
                </h1>
                {currentNote && (
                  <p className="text-sm text-gray-500">
                    Section 3.2 - Rich Text Editing & Manual Linking
                  </p>
                )}
                {!currentNote && activeTab === 'graph' && (
                  <p className="text-sm text-gray-500">
                    Section 3.3 - Graph Visualization
                  </p>
                )}
              </div>
            </div>

            {/* Header Actions */}
            <div className="flex items-center space-x-2">
              {/* Global Navigation - always show Graph button */}
              <div className="flex bg-gray-100 rounded-lg p-1">
                {currentNote ? (
                  <>
                    {/* Note-specific tabs */}
                    <button
                      onClick={() => setActiveTab('editor')}
                      className={`px-3 py-1.5 text-sm font-medium rounded-md transition-colors ${
                        activeTab === 'editor'
                          ? 'bg-white text-gray-900 shadow-sm'
                          : 'text-gray-500 hover:text-gray-700'
                      }`}
                    >
                      <svg className="h-4 w-4 inline mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                      </svg>
                      Editor
                    </button>
                    <button
                      onClick={() => setActiveTab('links')}
                      className={`px-3 py-1.5 text-sm font-medium rounded-md transition-colors ${
                        activeTab === 'links'
                          ? 'bg-white text-gray-900 shadow-sm'
                          : 'text-gray-500 hover:text-gray-700'
                      }`}
                    >
                      <svg className="h-4 w-4 inline mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
                      </svg>
                      Links ({(currentNote.outgoingLinks?.length || 0)})
                    </button>
                    <button
                      onClick={() => setActiveTab('tags')}
                      className={`px-3 py-1.5 text-sm font-medium rounded-md transition-colors ${
                        activeTab === 'tags'
                          ? 'bg-white text-gray-900 shadow-sm'
                          : 'text-gray-500 hover:text-gray-700'
                      }`}
                    >
                      <svg className="h-4 w-4 inline mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
                      </svg>
                      Tags ({currentNote.tags.length})
                    </button>
                  </>
                ) : null}
                
                {/* Graph tab - always available */}
                <button
                  onClick={() => {
                    setActiveTab('graph');
                    setCurrentNote(null); // Clear current note when viewing global graph
                  }}
                  className={`px-3 py-1.5 text-sm font-medium rounded-md transition-colors ${
                    activeTab === 'graph'
                      ? 'bg-white text-gray-900 shadow-sm'
                      : 'text-gray-500 hover:text-gray-700'
                  }`}
                >
                  <svg className="h-4 w-4 inline mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                  </svg>
                  Graph
                </button>
              </div>
            </div>
          </div>
        </header>

        {/* Error Display */}
        {error && (
          <div className="bg-red-50 border-l-4 border-red-400 p-4 mb-4 mx-4 mt-4">
            <div className="flex items-center justify-between">
              <div className="flex">
                <svg className="h-5 w-5 text-red-400 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <p className="text-sm text-red-700">{error}</p>
              </div>
              <button
                onClick={clearError}
                className="text-red-400 hover:text-red-600 transition-colors"
              >
                <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
          </div>
        )}

        {/* Main Content */}
        <main className="flex-1 flex min-h-0">
          {/* Primary Panel */}
          <div className="flex-1 flex flex-col min-w-0">
            {activeTab === 'graph' ? (
              <div className="flex-1 bg-white">
                <GraphVisualization />
              </div>
            ) : currentNote ? (
              <>
                {activeTab === 'editor' && (
                  <NoteEditor 
                    note={currentNote} 
                    className="flex-1"
                  />
                )}
                
                {activeTab === 'links' && (
                  <div className="flex-1 p-6 overflow-y-auto">
                    <NoteLinkPanel note={currentNote} />
                  </div>
                )}
                
                {activeTab === 'tags' && (
                  <div className="flex-1 p-6 overflow-y-auto">
                    <TagManager note={currentNote} />
                  </div>
                )}
              </>
            ) : (
              <div className="flex-1 flex items-center justify-center bg-white">
                <div className="text-center max-w-md">
                  <div className="mx-auto h-16 w-16 bg-blue-100 rounded-full flex items-center justify-center mb-6">
                    <svg className="h-8 w-8 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                  </div>
                  
                  <h2 className="text-2xl font-bold text-gray-900 mb-2">
                    Welcome to Realm Notes
                  </h2>
                  
                  <p className="text-gray-600 mb-6">
                    Create rich, interconnected notes with powerful linking and organization features.
                    {!sidebarOpen && ' Open the sidebar to get started, or explore your knowledge graph.'}
                  </p>

                  <div className="flex flex-col sm:flex-row gap-3 justify-center">
                    {!sidebarOpen && (
                      <button
                        onClick={toggleSidebar}
                        className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                      >
                        <svg className="h-4 w-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                        </svg>
                        Open Sidebar
                      </button>
                    )}
                    
                    <button
                      onClick={() => setActiveTab('graph')}
                      className="inline-flex items-center px-4 py-2 border border-blue-600 text-sm font-medium rounded-md text-blue-600 bg-white hover:bg-blue-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                    >
                      <svg className="h-4 w-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                      </svg>
                      View Graph
                    </button>
                  </div>

                  <div className="mt-8 text-sm text-gray-500">
                    <div className="grid grid-cols-1 gap-4 sm:grid-cols-4">
                      <div className="flex items-center">
                        <svg className="h-5 w-5 text-green-500 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                        </svg>
                        Rich text editing
                      </div>
                      <div className="flex items-center">
                        <svg className="h-5 w-5 text-green-500 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                        </svg>
                        Manual linking
                      </div>
                      <div className="flex items-center">
                        <svg className="h-5 w-5 text-green-500 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                        </svg>
                        Graph visualization
                      </div>
                      <div className="flex items-center">
                        <svg className="h-5 w-5 text-green-500 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                        </svg>
                        Auto-save
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        </main>

        {/* Loading Overlay */}
        {isLoading && (
          <div className="absolute inset-0 bg-white bg-opacity-75 flex items-center justify-center z-50">
            <div className="text-center">
              <div className="animate-spin h-8 w-8 border-4 border-blue-600 rounded-full border-t-transparent mx-auto mb-4"></div>
              <p className="text-gray-600">Loading...</p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};