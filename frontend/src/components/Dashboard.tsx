// Dashboard Component for Section 3.2 - Rich Text Editing & Manual Linking
// Main application interface with integrated note management system

import React from 'react';
import {useAuth} from '../contexts/AuthContext';
import {NoteManagement} from './NoteManagement';
import {QueryProvider} from '../providers/QueryProvider';

export const Dashboard: React.FC = () => {
  const { user, logout } = useAuth();

  const handleLogout = () => {
    logout();
  };

  return (
    <QueryProvider>
      <div className="min-h-screen bg-gray-50">
        {/* Header */}
        <header className="bg-white shadow-sm border-b border-gray-200 relative z-10">
          <div className="px-4 sm:px-6 lg:px-8">
            <div className="flex justify-between items-center py-4">
              <div className="flex items-center">
                <div className="h-8 w-8 bg-blue-600 rounded-full flex items-center justify-center mr-3">
                  <svg 
                    className="h-4 w-4 text-white" 
                    fill="none" 
                    stroke="currentColor" 
                    viewBox="0 0 24 24"
                  >
                    <path 
                      strokeLinecap="round" 
                      strokeLinejoin="round" 
                      strokeWidth={2} 
                      d="M13 10V3L4 14h7v7l9-11h-7z" 
                    />
                  </svg>
                </div>
                <div>
                  <h1 className="text-xl font-bold text-gray-900">
                    Realm
                  </h1>
                  <p className="text-xs text-gray-500">
                    Graph-based Personal Knowledge Management
                  </p>
                </div>
              </div>
              
              <div className="flex items-center space-x-4">
                <div className="text-sm text-gray-600 hidden sm:block">
                  <div className="flex items-center space-x-2">
                    <div className="h-2 w-2 bg-green-400 rounded-full"></div>
                    <span>{user?.displayName}</span>
                  </div>
                </div>
                
                <div className="relative">
                  <button
                    onClick={handleLogout}
                    className="inline-flex items-center px-3 py-2 border border-gray-300 shadow-sm text-sm leading-4 font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors duration-200"
                    title="Account menu"
                  >
                    <svg className="h-4 w-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                    </svg>
                    Sign Out
                  </button>
                </div>
              </div>
            </div>
          </div>
        </header>

        {/* Main Application */}
        <div className="h-screen pt-[73px]"> {/* Account for header height */}
          <NoteManagement />
        </div>

        {/* Footer/Status Bar */}
        <div className="fixed bottom-0 left-0 right-0 bg-gray-800 text-gray-300 px-4 py-2 text-xs flex items-center justify-between z-20">
          <div className="flex items-center space-x-4">
            <div className="flex items-center space-x-2">
              <div className="h-2 w-2 bg-green-400 rounded-full"></div>
              <span>Section 3.2 Active</span>
            </div>
            <span className="hidden sm:inline">Rich Text Editing & Manual Linking</span>
          </div>
          
          <div className="flex items-center space-x-4">
            <span className="hidden sm:inline">
              Connected as {user?.displayName}
            </span>
            <span>
              {new Date().toLocaleTimeString([], { 
                hour: '2-digit', 
                minute: '2-digit' 
              })}
            </span>
          </div>
        </div>
      </div>
    </QueryProvider>
  );
};