// Create page inspired by Craft's clean creation interface
// Provides options for document creation and quick access

import React, {useState} from 'react';
import {motion} from 'framer-motion';
import {BookOpen, Clock, FileText, Plus, Search, Zap} from 'lucide-react';
import {Note} from '../types/note';

interface CreatePageProps {
  recentDocuments: Note[];
  onCreateDocument: () => void;
  onDocumentSelect: (doc: Note) => void;
  onQuickOpen: (query: string) => void;
}

export const CreatePage: React.FC<CreatePageProps> = ({
  recentDocuments,
  onCreateDocument,
  onDocumentSelect,
  onQuickOpen
}) => {
  const [searchQuery, setSearchQuery] = useState('');

  const handleQuickSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      onQuickOpen(searchQuery.trim());
    }
  };

  const formatDate = (date: string) => {
    const dateObj = new Date(date);
    const now = new Date();
    const diffTime = Math.abs(now.getTime() - dateObj.getTime());
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    if (diffDays === 1) return 'Yesterday';
    if (diffDays <= 7) return `${diffDays} days ago`;
    return dateObj.toLocaleDateString('en-US', { 
      month: 'short', 
      day: 'numeric'
    });
  };

  const creationOptions = [
    {
      id: 'new-doc',
      title: 'New Document',
      description: 'Start writing with a blank document',
      icon: FileText,
      action: onCreateDocument,
      primary: true
    },
    {
      id: 'quick-note',
      title: 'Quick Note',
      description: 'Jot down a quick thought or idea',
      icon: Zap,
      action: onCreateDocument,
      primary: false
    },
    {
      id: 'from-template',
      title: 'From Template',
      description: 'Create from a predefined template',
      icon: BookOpen,
      action: () => {}, // TODO: Implement templates
      primary: false,
      disabled: true
    }
  ];

  return (
    <div className="flex flex-col h-full bg-gray-50">
      <div className="flex-1 flex items-center justify-center p-8">
        <div className="w-full max-w-2xl">
          {/* Header */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="text-center mb-12"
          >
            <h1 className="text-3xl font-bold text-gray-900 mb-3">Create</h1>
            <p className="text-gray-600">Start a new document or find what you're looking for</p>
          </motion.div>

          {/* Quick Search */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
            className="mb-8"
          >
            <form onSubmit={handleQuickSearch} className="relative">
              <Search className="absolute left-4 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
              <input
                type="text"
                placeholder="Quick Open - Search documents or create new..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="
                  w-full pl-12 pr-4 py-4 bg-white border border-gray-200 rounded-xl
                  focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500
                  text-lg transition-all duration-200 shadow-sm
                  hover:shadow-md focus:shadow-lg
                "
              />
              <div className="absolute right-4 top-1/2 transform -translate-y-1/2 text-xs text-gray-400">
                Press Enter
              </div>
            </form>
          </motion.div>

          {/* Creation Options */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
            className="grid gap-4 mb-8"
          >
            {creationOptions.map((option, index) => {
              const Icon = option.icon;
              return (
                <motion.button
                  key={option.id}
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: 0.1 + index * 0.05 }}
                  onClick={option.action}
                  disabled={option.disabled}
                  className={`
                    flex items-center p-6 rounded-xl border text-left transition-all duration-200
                    ${option.primary
                      ? 'bg-blue-50 border-blue-200 hover:bg-blue-100 hover:border-blue-300'
                      : 'bg-white border-gray-200 hover:bg-gray-50 hover:border-gray-300'
                    }
                    ${option.disabled
                      ? 'opacity-50 cursor-not-allowed'
                      : 'cursor-pointer hover:shadow-md'
                    }
                    group
                  `}
                >
                  <div className={`
                    flex-shrink-0 w-12 h-12 rounded-lg flex items-center justify-center mr-4
                    ${option.primary 
                      ? 'bg-blue-100 text-blue-600' 
                      : 'bg-gray-100 text-gray-600'
                    }
                    ${!option.disabled && 'group-hover:scale-110'}
                    transition-transform duration-200
                  `}>
                    <Icon className="h-6 w-6" />
                  </div>
                  <div className="flex-1">
                    <h3 className={`font-semibold mb-1 ${
                      option.primary ? 'text-blue-900' : 'text-gray-900'
                    }`}>
                      {option.title}
                    </h3>
                    <p className="text-sm text-gray-600">{option.description}</p>
                  </div>
                  {!option.disabled && (
                    <Plus className={`h-5 w-5 ${
                      option.primary ? 'text-blue-500' : 'text-gray-400'
                    } opacity-0 group-hover:opacity-100 transition-opacity duration-200`} />
                  )}
                </motion.button>
              );
            })}
          </motion.div>

          {/* Recent Documents */}
          {recentDocuments.length > 0 && (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.3 }}
              className="bg-white rounded-xl border border-gray-200 p-6"
            >
              <div className="flex items-center mb-4">
                <Clock className="h-5 w-5 text-gray-600 mr-2" />
                <h3 className="font-semibold text-gray-900">Recently Opened</h3>
              </div>
              
              <div className="space-y-2">
                {recentDocuments.slice(0, 5).map((doc, index) => (
                  <motion.button
                    key={doc.id}
                    initial={{ opacity: 0, x: -10 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.05 + index * 0.05 }}
                    onClick={() => onDocumentSelect(doc)}
                    className="
                      w-full flex items-center p-3 rounded-lg
                      hover:bg-gray-50 transition-colors duration-150
                      text-left group
                    "
                  >
                    <div className="flex-shrink-0 w-8 h-8 bg-gray-100 rounded-md flex items-center justify-center mr-3">
                      <FileText className="h-4 w-4 text-gray-600" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="font-medium text-gray-900 truncate group-hover:text-blue-600 transition-colors duration-150">
                        {doc.title || 'Untitled'}
                      </p>
                      <p className="text-xs text-gray-500">
                        {formatDate(doc.updatedAt)}
                      </p>
                    </div>
                  </motion.button>
                ))}
              </div>
            </motion.div>
          )}
        </div>
      </div>
    </div>
  );
};