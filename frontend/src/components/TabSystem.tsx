// Tab-based navigation system inspired by Craft
// Handles Home, Graph, and dynamic document tabs

import React from 'react';
import {AnimatePresence, motion} from 'framer-motion';
import {Home, Plus, Share2, X} from 'lucide-react';

export interface Tab {
  id: string;
  type: 'home' | 'graph' | 'create' | 'document';
  title: string;
  isCloseable: boolean;
  content?: any;
}

interface TabSystemProps {
  tabs: Tab[];
  activeTabId: string;
  onTabSelect: (tabId: string) => void;
  onTabClose: (tabId: string) => void;
  onCreateDocument: () => void;
}

export const TabSystem: React.FC<TabSystemProps> = ({
  tabs,
  activeTabId,
  onTabSelect,
  onTabClose,
  onCreateDocument
}) => {
  const getTabIcon = (tab: Tab) => {
    switch (tab.type) {
      case 'home':
        return <Home className="h-4 w-4" />;
      case 'graph':
        return <Share2 className="h-4 w-4" />;
      case 'create':
        return <Plus className="h-4 w-4" />;
      default:
        return null;
    }
  };

  return (
    <div className="flex flex-col h-full bg-white">
      {/* Tab Bar */}
      <div className="flex items-center bg-gray-50 border-b border-gray-200 px-1 py-1">
        <div className="flex items-center space-x-0.5 flex-1">
          {tabs.map((tab) => {
            const isActive = tab.id === activeTabId;
            
            return (
              <motion.div
                key={tab.id}
                layout
                className={`
                  flex items-center space-x-2 px-3 py-1.5 rounded-md text-sm font-medium
                  cursor-pointer transition-colors duration-150
                  ${isActive 
                    ? 'bg-white text-gray-900 shadow-sm border border-gray-200' 
                    : 'text-gray-600 hover:text-gray-900 hover:bg-white/50'
                  }
                `}
                onClick={() => onTabSelect(tab.id)}
              >
                {getTabIcon(tab)}
                <span className="truncate max-w-32">{tab.title}</span>
                
                {tab.isCloseable && (
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      onTabClose(tab.id);
                    }}
                    className={`
                      p-0.5 rounded hover:bg-gray-200 transition-colors duration-150
                      ${isActive ? 'text-gray-500 hover:text-gray-700' : 'text-gray-400'}
                    `}
                  >
                    <X className="h-3 w-3" />
                  </button>
                )}
              </motion.div>
            );
          })}
        </div>
        
        {/* New Document Button */}
        <button
          onClick={onCreateDocument}
          className="
            flex items-center justify-center w-8 h-8 rounded-md
            text-gray-600 hover:text-gray-900 hover:bg-white
            transition-colors duration-150
          "
        >
          <Plus className="h-4 w-4" />
        </button>
      </div>

      {/* Tab Content */}
      <div className="flex-1 overflow-hidden">
        <AnimatePresence mode="wait">
          {tabs.map((tab) => {
            if (tab.id !== activeTabId) return null;
            
            return (
              <motion.div
                key={tab.id}
                initial={{ opacity: 0, y: 4 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -4 }}
                transition={{ duration: 0.15, ease: 'easeOut' }}
                className="h-full"
              >
                {tab.content}
              </motion.div>
            );
          })}
        </AnimatePresence>
      </div>
    </div>
  );
};