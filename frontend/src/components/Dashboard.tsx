// Dashboard Component for Section 3.3 - Graph Visualization
// Main application interface with integrated note management and graph visualization

import React, {useState} from 'react';
import {useAuth} from '../contexts/AuthContext';
import {NoteManagement} from './NoteManagement';
import {GraphVisualization} from './GraphVisualization';
import {QueryProvider} from '../providers/QueryProvider';

type ActiveView = 'notes' | 'graph';

export const Dashboard: React.FC = () => {
  const { user, logout } = useAuth();
  const [activeView, setActiveView] = useState<ActiveView>('notes');

  const handleLogout = () => {
    logout();
  };

  return (
    <QueryProvider>
      <div className="min-h-screen bg-gray-50">
        {/* Header */}
        <header className="bg-white shadow-sm border-b border-gray-200 relative z-10">
          <div className="px-4 sm:px-6 lg:px-8">
            <div className="flex justify-between items-center py-2 sm:py-4">
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
                  <p className="text-xs text-gray-500 hidden sm:block">
                    Graph-based Personal Knowledge Management
                  </p>
                </div>
              </div>

              {/* Navigation Tabs */}
              <div className="flex items-center space-x-1 bg-gray-100 rounded-lg p-1">
                <button
                  onClick={() => setActiveView('notes')}
                  className={`flex items-center px-2 sm:px-3 py-1.5 rounded-md text-sm font-medium transition-all ${
                    activeView === 'notes'
                      ? 'bg-white text-blue-700 shadow-sm'
                      : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
                  }`}
                >
                  <svg className="w-4 h-4 mr-1 sm:mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                  <span className="hidden sm:inline">Notes</span>
                </button>
                <button
                  onClick={() => setActiveView('graph')}
                  className={`flex items-center px-2 sm:px-3 py-1.5 rounded-md text-sm font-medium transition-all ${
                    activeView === 'graph'
                      ? 'bg-white text-blue-700 shadow-sm'
                      : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
                  }`}
                >
                  <svg className="w-4 h-4 mr-1 sm:mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14-7H5m14 14H5" />
                  </svg>
                  <span className="hidden sm:inline">Graph</span>
                </button>
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
          {activeView === 'notes' ? (
            <NoteManagement />
          ) : (
            <div className="h-full">
              <GraphVisualization />
            </div>
          )}
        </div>

        {/* Footer/Status Bar */}
        <div className="fixed bottom-0 left-0 right-0 bg-gray-800 text-gray-300 px-4 py-2 text-xs flex items-center justify-between z-20">
          <div className="flex items-center space-x-4">
            <div className="flex items-center space-x-2">
              <div className="h-2 w-2 bg-green-400 rounded-full"></div>
              <span>Section 3.3 Active</span>
            </div>
            <span className="hidden sm:inline">
              {activeView === 'notes' ? 'Note Management' : 'Graph Visualization'}
            </span>
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