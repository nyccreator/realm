// Main Craft-inspired Dashboard with tab-based navigation
// Integrates all new components into a cohesive experience

import React, {useCallback, useState} from 'react';
import {QueryProvider} from '../providers/QueryProvider';
import {useNoteStore} from '../stores/noteStore';
import {useAuth} from '../contexts/AuthContext';
import {Note} from '../types/note';

import {Tab, TabSystem} from './TabSystem';
import {HomePage} from './HomePage';
import {CreatePage} from './CreatePage';
import {DocumentEditor} from './DocumentEditor';
import {ModernGraphViewWithProvider} from './ModernGraphView';

interface CraftDashboardState {
  tabs: Tab[];
  activeTabId: string;
}

export const CraftDashboard: React.FC = () => {
  const { user, logout } = useAuth();
  const { 
    notes, 
    isLoading, 
    error,
    loadNotes,
    selectNote,
    createNote 
  } = useNoteStore();

  // Tab management state
  const [dashboardState, setDashboardState] = useState<CraftDashboardState>(() => ({
    tabs: [
      {
        id: 'home',
        type: 'home',
        title: 'Home',
        isCloseable: false,
        content: null
      },
      {
        id: 'graph',
        type: 'graph',
        title: 'Graph',
        isCloseable: false,
        content: null
      },
      {
        id: 'create',
        type: 'create',
        title: 'Create',
        isCloseable: false,
        content: null
      }
    ],
    activeTabId: 'home'
  }));

  // Load notes on mount
  React.useEffect(() => {
    loadNotes();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // Handle document selection
  const handleDocumentSelect = useCallback((note: Note) => {
    const documentTabId = `doc-${note.id}`;
    
    // Check if document tab already exists
    const existingTab = dashboardState.tabs.find(t => t.id === documentTabId);
    
    if (existingTab) {
      // Switch to existing tab
      setDashboardState(prev => ({
        ...prev,
        activeTabId: documentTabId
      }));
    } else {
      // Create new document tab
      const newTab: Tab = {
        id: documentTabId,
        type: 'document',
        title: note.title || 'Untitled',
        isCloseable: true,
        content: null
      };
      
      setDashboardState(prev => ({
        tabs: [...prev.tabs, newTab],
        activeTabId: documentTabId
      }));
    }
    
    // Update selected note in store
    selectNote(note.id);
  }, [dashboardState.tabs, selectNote]);

  // Handle document creation
  const handleCreateDocument = useCallback(async () => {
    try {
      const newNote = await createNote({
        title: 'Untitled Document',
        content: '',
        tags: []
      });
      
      if (newNote) {
        handleDocumentSelect(newNote);
      }
    } catch (error) {
      console.error('Failed to create document:', error);
    }
  }, [createNote, handleDocumentSelect]);

  // Handle tab selection
  const handleTabSelect = useCallback((tabId: string) => {
    setDashboardState(prev => ({
      ...prev,
      activeTabId: tabId
    }));
  }, []);

  // Handle tab close
  const handleTabClose = useCallback((tabId: string) => {
    setDashboardState(prev => {
      const newTabs = prev.tabs.filter(t => t.id !== tabId);
      const newActiveTabId = prev.activeTabId === tabId 
        ? (newTabs.length > 0 ? newTabs[newTabs.length - 1].id : 'home')
        : prev.activeTabId;
      
      return {
        tabs: newTabs,
        activeTabId: newActiveTabId
      };
    });
  }, []);

  // Handle search
  const handleSearch = useCallback((query: string) => {
    // TODO: Implement search functionality
    console.log('Search:', query);
  }, []);

  // Handle quick open
  const handleQuickOpen = useCallback((query: string) => {
    // Switch to home tab and trigger search
    setDashboardState(prev => ({
      ...prev,
      activeTabId: 'home'
    }));
    handleSearch(query);
  }, [handleSearch]);

  // Generate tab content dynamically
  const getTabContent = (tab: Tab) => {
    switch (tab.type) {
      case 'home':
        return (
          <HomePage
            documents={notes}
            onDocumentSelect={handleDocumentSelect}
            onCreateDocument={handleCreateDocument}
            onSearch={handleSearch}
          />
        );
      
      case 'graph':
        return (
          <ModernGraphViewWithProvider 
            onNodeSelect={handleDocumentSelect}
          />
        );
      
      case 'create':
        return (
          <CreatePage
            recentDocuments={notes.slice(0, 5)}
            onCreateDocument={handleCreateDocument}
            onDocumentSelect={handleDocumentSelect}
            onQuickOpen={handleQuickOpen}
          />
        );
      
      case 'document':
        const note = notes.find(n => n.id === tab.id.replace('doc-', ''));
        return note ? (
          <DocumentEditor note={note} />
        ) : (
          <div className="flex items-center justify-center h-full">
            <p className="text-gray-500">Document not found</p>
          </div>
        );
      
      default:
        return null;
    }
  };

  const tabsWithContent = dashboardState.tabs.map(tab => ({
    ...tab,
    content: getTabContent(tab)
  }));

  if (isLoading && notes.length === 0) {
    return (
      <div className="h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading your workspace...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="w-16 h-16 bg-red-50 rounded-full flex items-center justify-center mb-4 mx-auto">
            <svg className="h-8 w-8 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <h3 className="text-lg font-medium text-gray-900 mb-2">Something went wrong</h3>
          <p className="text-gray-600 mb-4">{String(error)}</p>
          <button
            onClick={() => loadNotes()}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            Try Again
          </button>
        </div>
      </div>
    );
  }

  return (
    <QueryProvider>
      <div className="h-screen bg-gray-50 flex flex-col">
        {/* User Menu - Minimal top bar */}
        <div className="flex-shrink-0 bg-white border-b border-gray-200 px-4 py-2">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="flex items-center space-x-2">
                <div className="w-6 h-6 bg-gradient-to-br from-blue-500 to-purple-600 rounded-md flex items-center justify-center">
                  <span className="text-white font-bold text-xs">R</span>
                </div>
                <span className="font-semibold text-gray-900 text-sm">Realm</span>
              </div>
              
              {/* Breadcrumb */}
              <div className="flex items-center space-x-2 text-sm text-gray-500">
                <span>/</span>
                <span className="capitalize">
                  {tabsWithContent.find(t => t.id === dashboardState.activeTabId)?.title || 'Home'}
                </span>
              </div>
            </div>

            <div className="flex items-center space-x-3">
              <div className="flex items-center space-x-2 text-sm text-gray-600">
                <span>{user?.displayName}</span>
              </div>
              
              <button
                onClick={logout}
                className="
                  text-sm text-gray-600 hover:text-gray-900 
                  px-2 py-1 rounded hover:bg-gray-100
                  transition-colors duration-150
                "
              >
                Sign out
              </button>
            </div>
          </div>
        </div>

        {/* Main Content with Tab System */}
        <div className="flex-1 overflow-hidden">
          <TabSystem
            tabs={tabsWithContent}
            activeTabId={dashboardState.activeTabId}
            onTabSelect={handleTabSelect}
            onTabClose={handleTabClose}
            onCreateDocument={handleCreateDocument}
          />
        </div>
      </div>
    </QueryProvider>
  );
};