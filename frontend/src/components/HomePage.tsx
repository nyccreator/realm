// Home page with document grid layout inspired by Craft
// Shows document previews with thumbnails and recent documents

import React, {useState} from 'react';
import {motion} from 'framer-motion';
import {Clock, FileText, Grid, List, Plus, Search} from 'lucide-react';
import {Note} from '../types/note';

interface DocumentCard {
  id: string | number;
  title: string;
  preview: string;
  lastModified: Date;
  type: 'document' | 'note';
  thumbnail?: string;
}

interface HomePageProps {
  documents: Note[];
  onDocumentSelect: (doc: Note) => void;
  onCreateDocument: () => void;
  onSearch: (query: string) => void;
}

export const HomePage: React.FC<HomePageProps> = ({
  documents,
  onDocumentSelect,
  onCreateDocument,
  onSearch
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');

  // Convert notes to document cards format
  const documentCards: DocumentCard[] = documents.map(note => ({
    id: note.id,
    title: note.title || 'Untitled',
    preview: note.content?.substring(0, 150) || 'No content',
    lastModified: new Date(note.updatedAt),
    type: 'document'
  }));

  // Filter documents based on search
  const filteredDocuments = documentCards.filter(doc =>
    doc.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
    doc.preview.toLowerCase().includes(searchQuery.toLowerCase())
  );

  // Group documents by date
  const groupedDocs = {
    recent: filteredDocuments.filter(doc => {
      const daysDiff = (Date.now() - doc.lastModified.getTime()) / (1000 * 60 * 60 * 24);
      return daysDiff <= 7;
    }),
    older: filteredDocuments.filter(doc => {
      const daysDiff = (Date.now() - doc.lastModified.getTime()) / (1000 * 60 * 60 * 24);
      return daysDiff > 7;
    })
  };

  const formatDate = (date: Date) => {
    const now = new Date();
    const diffTime = Math.abs(now.getTime() - date.getTime());
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    if (diffDays === 1) return 'Yesterday';
    if (diffDays <= 7) return `${diffDays} days ago`;
    return date.toLocaleDateString('en-US', { 
      month: 'short', 
      day: 'numeric',
      year: date.getFullYear() !== now.getFullYear() ? 'numeric' : undefined
    });
  };

  const DocumentCard: React.FC<{ document: DocumentCard }> = ({ document }) => {
    const note = documents.find(n => n.id.toString() === document.id.toString());
    
    return (
      <motion.div
        layout
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        whileHover={{ y: -2, transition: { duration: 0.2 } }}
        className="
          bg-white rounded-xl border border-gray-200 overflow-hidden cursor-pointer
          hover:border-gray-300 hover:shadow-md transition-all duration-200
          group
        "
        onClick={() => note && onDocumentSelect(note)}
      >
        {/* Document Thumbnail/Preview */}
        <div className="h-32 bg-gradient-to-br from-gray-50 to-gray-100 p-4 relative overflow-hidden">
          <div className="text-sm text-gray-700 line-clamp-4 leading-relaxed">
            {document.preview}
          </div>
          <div className="absolute bottom-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200">
            <FileText className="h-4 w-4 text-gray-400" />
          </div>
        </div>
        
        {/* Document Info */}
        <div className="p-4">
          <h3 className="font-semibold text-gray-900 mb-1 truncate group-hover:text-blue-600 transition-colors duration-200">
            {document.title}
          </h3>
          <p className="text-xs text-gray-500 flex items-center">
            <Clock className="h-3 w-3 mr-1" />
            {formatDate(document.lastModified)}
          </p>
        </div>
      </motion.div>
    );
  };

  const DocumentListItem: React.FC<{ document: DocumentCard }> = ({ document }) => {
    const note = documents.find(n => n.id.toString() === document.id.toString());
    
    return (
      <motion.div
        layout
        initial={{ opacity: 0, x: -20 }}
        animate={{ opacity: 1, x: 0 }}
        className="
          bg-white rounded-lg border border-gray-200 p-4 cursor-pointer
          hover:border-gray-300 hover:shadow-sm transition-all duration-200
          group
        "
        onClick={() => note && onDocumentSelect(note)}
      >
        <div className="flex items-start space-x-3">
          <div className="flex-shrink-0 w-10 h-10 bg-gray-100 rounded-lg flex items-center justify-center">
            <FileText className="h-5 w-5 text-gray-600" />
          </div>
          <div className="flex-1 min-w-0">
            <h3 className="font-medium text-gray-900 truncate group-hover:text-blue-600 transition-colors duration-200">
              {document.title}
            </h3>
            <p className="text-sm text-gray-600 line-clamp-2 mt-1">
              {document.preview}
            </p>
            <p className="text-xs text-gray-500 mt-2 flex items-center">
              <Clock className="h-3 w-3 mr-1" />
              {formatDate(document.lastModified)}
            </p>
          </div>
        </div>
      </motion.div>
    );
  };

  return (
    <div className="flex flex-col h-full bg-gray-50">
      {/* Header */}
      <div className="bg-white border-b border-gray-200 p-6">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Home</h1>
            <p className="text-gray-600 mt-1">Your documents and recent work</p>
          </div>
          
          <div className="flex items-center space-x-3">
            <div className="flex items-center bg-gray-100 rounded-lg p-1">
              <button
                onClick={() => setViewMode('grid')}
                className={`p-2 rounded-md transition-colors duration-150 ${
                  viewMode === 'grid' 
                    ? 'bg-white text-gray-900 shadow-sm' 
                    : 'text-gray-500 hover:text-gray-700'
                }`}
              >
                <Grid className="h-4 w-4" />
              </button>
              <button
                onClick={() => setViewMode('list')}
                className={`p-2 rounded-md transition-colors duration-150 ${
                  viewMode === 'list' 
                    ? 'bg-white text-gray-900 shadow-sm' 
                    : 'text-gray-500 hover:text-gray-700'
                }`}
              >
                <List className="h-4 w-4" />
              </button>
            </div>
            
            <button
              onClick={onCreateDocument}
              className="
                bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg
                flex items-center space-x-2 transition-colors duration-200
                font-medium text-sm
              "
            >
              <Plus className="h-4 w-4" />
              <span>New Document</span>
            </button>
          </div>
        </div>

        {/* Search */}
        <div className="relative max-w-md">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
          <input
            type="text"
            placeholder="Search documents..."
            value={searchQuery}
            onChange={(e) => {
              setSearchQuery(e.target.value);
              onSearch(e.target.value);
            }}
            className="
              w-full pl-10 pr-4 py-2 bg-gray-50 border border-gray-200 rounded-lg
              focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500
              text-sm transition-colors duration-200
            "
          />
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-auto">
        {/* Empty State */}
        {filteredDocuments.length === 0 && (
          <div className="flex flex-col items-center justify-center h-full text-center p-8">
            <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4">
              <FileText className="h-8 w-8 text-gray-400" />
            </div>
            <h3 className="text-lg font-medium text-gray-900 mb-2">
              {searchQuery ? 'No documents found' : 'No documents yet'}
            </h3>
            <p className="text-gray-600 mb-6 max-w-sm">
              {searchQuery 
                ? `No documents match "${searchQuery}". Try a different search term.`
                : 'Create your first document to get started with your knowledge base.'
              }
            </p>
            <button
              onClick={onCreateDocument}
              className="
                bg-blue-600 hover:bg-blue-700 text-white px-6 py-3 rounded-lg
                flex items-center space-x-2 transition-colors duration-200
                font-medium
              "
            >
              <Plus className="h-4 w-4" />
              <span>Create Document</span>
            </button>
          </div>
        )}

        {/* Documents */}
        {filteredDocuments.length > 0 && (
          <div className="p-6 space-y-8">
            {/* Recent Documents */}
            {groupedDocs.recent.length > 0 && (
              <section>
                <h2 className="text-lg font-semibold text-gray-900 mb-4">Recent</h2>
                <div className={
                  viewMode === 'grid' 
                    ? 'grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4'
                    : 'space-y-3'
                }>
                  {groupedDocs.recent.map((doc) => (
                    viewMode === 'grid' 
                      ? <DocumentCard key={doc.id} document={doc} />
                      : <DocumentListItem key={doc.id} document={doc} />
                  ))}
                </div>
              </section>
            )}

            {/* Older Documents */}
            {groupedDocs.older.length > 0 && (
              <section>
                <h2 className="text-lg font-semibold text-gray-900 mb-4">Older</h2>
                <div className={
                  viewMode === 'grid' 
                    ? 'grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4'
                    : 'space-y-3'
                }>
                  {groupedDocs.older.map((doc) => (
                    viewMode === 'grid' 
                      ? <DocumentCard key={doc.id} document={doc} />
                      : <DocumentListItem key={doc.id} document={doc} />
                  ))}
                </div>
              </section>
            )}
          </div>
        )}
      </div>
    </div>
  );
};