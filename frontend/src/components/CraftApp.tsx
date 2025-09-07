// Main Craft-inspired Application Component
// Clean, tab-based interface inspired by Craft's design language

import React, {useCallback, useState} from 'react';
import {User} from '../types/auth';

// Main Layout Components
import {CraftTabSystem} from './CraftTabSystem';
import {CraftHome} from './CraftHome';
import {CraftCreate} from './CraftCreate';
import {CraftGraph} from './CraftGraph';
import {CraftDocument} from './CraftDocument';

interface Tab {
  id: string;
  type: 'home' | 'create' | 'graph' | 'document';
  title: string;
  isCloseable: boolean;
  documentId?: string;
}

interface CraftAppProps {
  user: User;
  onLogout: () => Promise<void>;
}

export const CraftApp: React.FC<CraftAppProps> = ({ user, onLogout }) => {
  const [tabs, setTabs] = useState<Tab[]>([
    {
      id: 'home',
      type: 'home',
      title: 'Home',
      isCloseable: false,
    },
  ]);
  const [activeTabId, setActiveTabId] = useState<string>('home');

  const handleTabSelect = useCallback((tabId: string) => {
    setActiveTabId(tabId);
  }, []);

  const handleTabClose = useCallback((tabId: string) => {
    setTabs(prev => {
      const newTabs = prev.filter(tab => tab.id !== tabId);
      // If we closed the active tab, switch to the last remaining tab
      if (activeTabId === tabId && newTabs.length > 0) {
        setActiveTabId(newTabs[newTabs.length - 1].id);
      }
      return newTabs;
    });
  }, [activeTabId]);

  const handleDocumentOpen = useCallback((documentId: string, title: string) => {
    const documentTabId = `doc-${documentId}`;
    
    // Check if document is already open
    const existingTab = tabs.find(tab => tab.id === documentTabId);
    
    if (existingTab) {
      // Switch to existing tab
      setActiveTabId(documentTabId);
    } else {
      // Create new document tab
      const newTab: Tab = {
        id: documentTabId,
        type: 'document',
        title: title || 'Untitled',
        isCloseable: true,
        documentId,
      };
      
      setTabs(prev => [...prev, newTab]);
      setActiveTabId(documentTabId);
    }
  }, [tabs]);

  const handleNewDocument = useCallback(() => {
    // Check if Create tab exists, if not create it
    const createTabExists = tabs.find(tab => tab.id === 'create');
    
    if (!createTabExists) {
      const createTab: Tab = {
        id: 'create',
        type: 'create',
        title: 'Create',
        isCloseable: false,
      };
      setTabs(prev => [...prev, createTab]);
    }
    
    setActiveTabId('create');
  }, [tabs]);

  const activeTab = tabs.find(tab => tab.id === activeTabId);

  const renderTabContent = () => {
    if (!activeTab) return null;

    switch (activeTab.type) {
      case 'home':
        return (
          <CraftHome 
            onDocumentOpen={handleDocumentOpen}
            onNewDocument={handleNewDocument}
          />
        );
      
      case 'create':
        return (
          <CraftCreate 
            onDocumentOpen={handleDocumentOpen}
            onNewDocument={handleNewDocument}
          />
        );
      
      case 'graph':
        return (
          <CraftGraph 
            onDocumentOpen={handleDocumentOpen}
          />
        );
      
      case 'document':
        return (
          <CraftDocument 
            documentId={activeTab.documentId!}
            onTitleChange={(newTitle) => {
              setTabs(prev => prev.map(tab => 
                tab.id === activeTab.id 
                  ? { ...tab, title: newTitle }
                  : tab
              ));
            }}
          />
        );
      
      default:
        return null;
    }
  };

  return (
    <div className="h-screen bg-white flex flex-col">
      {/* Top Bar with user info */}
      <div className="flex-shrink-0 bg-white border-b border-gray-200">
        <div className="flex items-center justify-between px-6 py-3">
          <div className="flex items-center space-x-4">
            {/* App Logo */}
            <div className="flex items-center space-x-2">
              <div className="w-6 h-6 bg-gradient-to-br from-blue-500 to-purple-600 rounded-md flex items-center justify-center">
                <span className="text-white font-bold text-xs">R</span>
              </div>
              <span className="font-semibold text-gray-900 text-sm">Realm</span>
            </div>
          </div>

          {/* User Menu */}
          <div className="flex items-center space-x-4">
            <div className="flex items-center space-x-2 text-sm text-gray-600">
              <span>{user.displayName}</span>
            </div>
            
            <button
              onClick={onLogout}
              className="text-sm text-gray-600 hover:text-gray-900 px-2 py-1 rounded hover:bg-gray-100 transition-colors duration-150"
            >
              Sign out
            </button>
          </div>
        </div>
      </div>

      {/* Tab System */}
      <CraftTabSystem
        tabs={tabs}
        activeTabId={activeTabId}
        onTabSelect={handleTabSelect}
        onTabClose={handleTabClose}
        onNewDocument={handleNewDocument}
      />

      {/* Main Content */}
      <div className="flex-1 overflow-hidden">
        {renderTabContent()}
      </div>
    </div>
  );
};