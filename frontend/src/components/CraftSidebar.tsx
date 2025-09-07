// Craft-inspired Sidebar Component
// Exact replica of Craft's sidebar navigation with Home, folders, and minimal icons

import React from 'react';
import {User} from '../types/auth';

type ViewType = 'home' | 'create' | 'graph' | 'document';

interface CraftSidebarProps {
  currentView: ViewType;
  onNavigate: (view: ViewType) => void;
  onNewDocument: () => void;
  user: User;
  onLogout: () => Promise<void>;
}

export const CraftSidebar: React.FC<CraftSidebarProps> = ({
  currentView,
  onNavigate,
  onNewDocument,
  user,
  onLogout
}) => {
  const sidebarItems = [
    {
      id: 'home',
      label: 'Home',
      icon: (
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2H5a2 2 0 00-2-2z" />
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 5a2 2 0 012-2h4a2 2 0 012 2v0M8 5a2 2 0 012-2h4a2 2 0 012 2v0" />
        </svg>
      ),
      view: 'home' as ViewType,
    },
    {
      id: 'create',
      label: 'Create',
      icon: (
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
        </svg>
      ),
      view: 'create' as ViewType,
    },
    {
      id: 'graph',
      label: 'Graph',
      icon: (
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
        </svg>
      ),
      view: 'graph' as ViewType,
    },
  ];

  return (
    <div className="w-60 bg-craft-sidebar border-r border-craft-border flex flex-col">
      {/* Sidebar Header */}
      <div className="p-4 border-b border-craft-border">
        <div className="flex items-center space-x-2">
          <div className="w-6 h-6 bg-gradient-to-br from-blue-500 to-purple-600 rounded-md flex items-center justify-center">
            <span className="text-white font-bold text-xs">R</span>
          </div>
          <span className="font-semibold text-craft-text text-sm">Realm</span>
        </div>
      </div>

      {/* Navigation Items */}
      <div className="flex-1 px-2 py-4 space-y-1">
        {sidebarItems.map((item) => (
          <button
            key={item.id}
            onClick={() => onNavigate(item.view)}
            className={`
              w-full flex items-center space-x-3 px-3 py-2 rounded-md text-sm font-medium transition-all duration-150
              ${currentView === item.view
                ? 'bg-craft-primary text-craft-primary-text shadow-sm'
                : 'text-craft-text-secondary hover:bg-craft-hover hover:text-craft-text'
              }
            `}
          >
            <span className={`
              ${currentView === item.view ? 'text-craft-primary-text' : 'text-craft-icon'}
            `}>
              {item.icon}
            </span>
            <span>{item.label}</span>
          </button>
        ))}
        
        {/* Folders Section */}
        <div className="pt-4 pb-2">
          <div className="px-3 py-1">
            <h3 className="text-xs font-medium text-craft-text-muted uppercase tracking-wider">
              Folders
            </h3>
          </div>
          
          {/* Sample folders - can be expanded later */}
          <div className="space-y-1">
            <button className="w-full flex items-center space-x-3 px-3 py-2 rounded-md text-sm text-craft-text-secondary hover:bg-craft-hover hover:text-craft-text transition-all duration-150">
              <svg className="w-4 h-4 text-craft-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2-2z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 5l4 4 4-4" />
              </svg>
              <span>Notes</span>
            </button>
            
            <button className="w-full flex items-center space-x-3 px-3 py-2 rounded-md text-sm text-craft-text-secondary hover:bg-craft-hover hover:text-craft-text transition-all duration-150">
              <svg className="w-4 h-4 text-craft-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2-2z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 5l4 4 4-4" />
              </svg>
              <span>Projects</span>
            </button>
          </div>
        </div>
      </div>

      {/* User Profile & Logout */}
      <div className="border-t border-craft-border p-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2 min-w-0 flex-1">
            <div className="w-6 h-6 bg-gradient-to-br from-gray-400 to-gray-600 rounded-full flex items-center justify-center">
              <span className="text-white font-medium text-xs">
                {user.displayName?.charAt(0).toUpperCase() || 'U'}
              </span>
            </div>
            <div className="min-w-0 flex-1">
              <p className="text-xs font-medium text-craft-text truncate">
                {user.displayName}
              </p>
              <p className="text-xs text-craft-text-muted truncate">
                {user.email}
              </p>
            </div>
          </div>
          
          <button
            onClick={onLogout}
            className="ml-2 p-1 text-craft-text-secondary hover:text-craft-text rounded-md hover:bg-craft-hover transition-all duration-150"
            title="Sign out"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  );
};