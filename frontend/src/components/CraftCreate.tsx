// Craft Create Page - Matches Image #2 exactly
// Centered quick action cards with search

import React, {useState} from 'react';

interface CraftCreateProps {
  onDocumentOpen: (documentId: string, title: string) => void;
  onNewDocument: () => void;
}

// Mock recently opened documents
const recentDocuments = [
  {
    id: '1',
    title: 'Getting Started',
    updatedAt: '2 months ago',
    icon: '📄',
  },
  {
    id: '2',
    title: 'Getting Started 👋',
    updatedAt: '4 months ago',
    icon: '📄',
  },
  {
    id: '3',
    title: 'Copy of Copy of Craft Handbook 📚',
    updatedAt: '5 months ago',
    icon: '📚',
  },
  {
    id: '4',
    title: 'Copy of Craft Handbook 📚',
    updatedAt: '5 months ago',
    icon: '📚',
  },
  {
    id: '5',
    title: 'How To Videos 📹',
    updatedAt: '5 months ago',
    icon: '📹',
  },
  {
    id: '6',
    title: 'Craft Handbook 📚',
    updatedAt: '5 months ago',
    icon: '📚',
  },
];

export const CraftCreate: React.FC<CraftCreateProps> = ({
  onDocumentOpen,
  onNewDocument,
}) => {
  const [searchQuery, setSearchQuery] = useState('');

  return (
    <div className="h-full bg-white overflow-auto">
      <div className="max-w-4xl mx-auto px-6 py-16">
        {/* Header */}
        <div className="text-center mb-12">
          <h1 className="text-4xl font-semibold text-gray-900 mb-4">Create</h1>
        </div>

        {/* Quick Action Cards */}
        <div className="flex justify-center mb-12">
          <div className="grid grid-cols-4 gap-4 max-w-2xl">
            {/* Quick Note */}
            <div 
              onClick={() => onDocumentOpen('new', 'Getting Started')}
              className="bg-white border border-gray-200 rounded-lg p-6 hover:border-gray-300 hover:shadow-sm transition-all duration-150 cursor-pointer text-center"
            >
              <div className="w-12 h-12 bg-red-100 rounded-lg flex items-center justify-center mx-auto mb-3">
                <span className="text-2xl">📝</span>
              </div>
              <h3 className="font-semibold text-gray-900 text-sm mb-1">Quick Note</h3>
              <p className="text-xs text-gray-500">Today</p>
            </div>

            {/* Upgrade Card */}
            <div className="bg-white border border-gray-200 rounded-lg p-6 hover:border-gray-300 hover:shadow-sm transition-all duration-150 cursor-pointer text-center">
              <div className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center mx-auto mb-3">
                <span className="text-xl">⭐</span>
              </div>
              <h3 className="font-semibold text-gray-900 text-sm mb-1">You can create</h3>
              <p className="text-xs text-blue-600 font-medium">2 more docs</p>
              <p className="text-xs text-gray-500 mt-1">Upgrade to Plus to get unlimited access.</p>
            </div>

            {/* Todo List */}
            <div className="bg-white border border-gray-200 rounded-lg p-6 hover:border-gray-300 hover:shadow-sm transition-all duration-150 cursor-pointer text-center">
              <div className="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center mx-auto mb-3">
                <svg className="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
                </svg>
              </div>
              <h3 className="font-semibold text-gray-900 text-sm">Todo List</h3>
            </div>

            {/* New Doc */}
            <div 
              onClick={() => onDocumentOpen('new', 'Getting Started')}
              className="bg-white border border-gray-200 rounded-lg p-6 hover:border-gray-300 hover:shadow-sm transition-all duration-150 cursor-pointer text-center"
            >
              <div className="w-12 h-12 bg-gray-100 rounded-lg flex items-center justify-center mx-auto mb-3">
                <svg className="w-6 h-6 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
              </div>
              <h3 className="font-semibold text-gray-900 text-sm mb-1">New Doc</h3>
              <p className="text-xs text-gray-500">what's on your mind?</p>
            </div>
          </div>
        </div>

        {/* Quick Open Search */}
        <div className="flex justify-center mb-12">
          <div className="relative max-w-md w-full">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <svg className="h-5 w-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>
            <input
              type="text"
              placeholder="Quick Open"
              className="block w-full pl-10 pr-3 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white text-gray-900 placeholder-gray-500 text-center"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
        </div>

        {/* Recently Opened Documents */}
        <div className="max-w-2xl mx-auto">
          <h2 className="text-lg font-medium text-gray-900 mb-6 text-center">Recently Opened Documents</h2>
          
          <div className="space-y-2">
            {recentDocuments.map((doc) => (
              <div
                key={doc.id}
                onClick={() => onDocumentOpen(doc.id, doc.title)}
                className="flex items-center p-3 rounded-lg hover:bg-gray-50 cursor-pointer transition-colors duration-150"
              >
                <div className="flex items-center justify-center w-8 h-8 mr-3">
                  <span className="text-lg">{doc.icon}</span>
                </div>
                <div className="flex-1">
                  <h3 className="font-medium text-gray-900 text-sm">{doc.title}</h3>
                </div>
                <div className="text-xs text-gray-500">
                  {doc.updatedAt}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};