// Craft-inspired Tab System Component
// macOS-style tabs with clean design

import React from 'react';

interface Tab {
  id: string;
  type: 'home' | 'create' | 'graph' | 'document';
  title: string;
  isCloseable: boolean;
  documentId?: string;
}

interface CraftTabSystemProps {
  tabs: Tab[];
  activeTabId: string;
  onTabSelect: (tabId: string) => void;
  onTabClose: (tabId: string) => void;
  onNewDocument: () => void;
}

export const CraftTabSystem: React.FC<CraftTabSystemProps> = ({
  tabs,
  activeTabId,
  onTabSelect,
  onTabClose,
  onNewDocument,
}) => {
  const getTabIcon = (type: string) => {
    switch (type) {
      case 'home':
        return (
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2H5a2 2 0 00-2-2z" />
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3a2 2 0 012-2h4a2 2 0 012 2v4M8 7v4a2 2 0 002 2h4a2 2 0 002-2V7" />
          </svg>
        );
      case 'graph':
        return (
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 12a9 9 0 01-9 9m9-9a9 9 0 00-9-9m9 9H3m9 9a9 9 0 01-9-9m9 9c1.657 0 3-4.03 3-9s-1.343-9-3-9m0 18c-1.657 0-3-4.03-3-9s1.343-9 3-9m-9 9a9 9 0 019-9" />
          </svg>
        );
      case 'document':
        return (
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
        );
      default:
        return null;
    }
  };

  return (
    <div className="flex-shrink-0 bg-white">
      <div className="flex items-center justify-center pt-4">
        {/* Craft-style Tab System */}
        <div className="flex items-center bg-gray-100 rounded-lg p-1">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              className={`
                flex items-center px-4 py-2 rounded-md text-sm font-medium transition-all duration-150
                ${
                  activeTabId === tab.id
                    ? 'bg-white text-gray-900 shadow-sm'
                    : 'text-gray-600 hover:text-gray-900'
                }
              `}
              onClick={() => onTabSelect(tab.id)}
            >
              {/* Tab Icon (smaller, only for specific tabs) */}
              {tab.type === 'home' && (
                <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
                </svg>
              )}
              
              {/* Tab Title */}
              <span className="truncate">
                {tab.title}
              </span>
              
              {/* Close Button for documents only */}
              {tab.isCloseable && (
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onTabClose(tab.id);
                  }}
                  className="ml-2 p-0.5 rounded hover:bg-gray-200 transition-colors duration-150"
                >
                  <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              )}
            </button>
          ))}
        </div>
        
        {/* New Tab Button (Craft style + icon) */}
        <button
          onClick={onNewDocument}
          className="ml-3 flex items-center justify-center w-8 h-8 text-gray-400 hover:text-gray-600 transition-colors duration-150"
          title="New Document"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
        </button>
      </div>
    </div>
  );
};