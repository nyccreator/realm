// Craft Home Page - Clean document grid with real backend data
// Simplified design focused on documents only

import React, {useEffect, useState} from 'react';

interface Document {
  id: string;
  title: string;
  content: string;
  updatedAt: string;
}

interface CraftHomeProps {
  onDocumentOpen: (documentId: string, title: string) => void;
  onNewDocument: () => void;
}

export const CraftHome: React.FC<CraftHomeProps> = ({
  onDocumentOpen,
  onNewDocument,
}) => {
  const [documents, setDocuments] = useState<Document[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchDocuments();
  }, []);

  const fetchDocuments = async () => {
    try {
      setIsLoading(true);
      const response = await fetch('/api/notes', {
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
      });

      if (!response.ok) {
        throw new Error('Failed to fetch documents');
      }

      const data = await response.json();
      setDocuments(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load documents');
      console.error('Error fetching documents:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffTime = now.getTime() - date.getTime();
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));

    if (diffDays === 0) {
      return 'Today';
    } else if (diffDays === 1) {
      return 'Yesterday';
    } else if (diffDays < 7) {
      return `${diffDays} days ago`;
    } else if (diffDays < 30) {
      const weeks = Math.floor(diffDays / 7);
      return weeks === 1 ? '1 week ago' : `${weeks} weeks ago`;
    } else if (diffDays < 365) {
      const months = Math.floor(diffDays / 30);
      return months === 1 ? '1 month ago' : `${months} months ago`;
    } else {
      return date.toLocaleDateString();
    }
  };

  const getContentPreview = (content: string) => {
    if (!content || content.trim() === '') {
      return 'No content';
    }
    return content.length > 100 ? content.substring(0, 100) + '...' : content;
  };

  const renderDocumentCard = (doc: Document) => (
    <div
      key={doc.id}
      onClick={() => onDocumentOpen(doc.id, doc.title)}
      className="group cursor-pointer"
    >
      <div className="bg-white border border-gray-200 rounded-lg hover:border-gray-300 hover:shadow-sm transition-all duration-150 overflow-hidden">
        {/* Document Preview Area */}
        <div className="h-32 bg-gray-50 border-b border-gray-100 p-3 flex items-start">
          <div className="w-full h-full bg-white rounded border border-gray-200 p-2">
            <div className="text-xs text-gray-600 leading-relaxed">
              {getContentPreview(doc.content)}
            </div>
          </div>
        </div>
        
        {/* Document Info */}
        <div className="p-3">
          <h3 className="font-medium text-gray-900 text-sm mb-1 line-clamp-1">
            {doc.title || 'Untitled'}
          </h3>
          <div className="flex items-center justify-between text-xs text-gray-500">
            <span>{formatDate(doc.updatedAt)}</span>
          </div>
        </div>
      </div>
    </div>
  );

  const renderEmptyState = () => (
    <div className="text-center py-16">
      <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
        <svg className="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
      </div>
      <h3 className="text-lg font-medium text-gray-900 mb-2">No documents yet</h3>
      <p className="text-gray-600 mb-6">Create your first document to get started</p>
      <button
        onClick={onNewDocument}
        className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors duration-150"
      >
        Create Document
      </button>
    </div>
  );

  const renderErrorState = () => (
    <div className="text-center py-16">
      <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
        <svg className="w-8 h-8 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z" />
        </svg>
      </div>
      <h3 className="text-lg font-medium text-gray-900 mb-2">Error loading documents</h3>
      <p className="text-gray-600 mb-6">{error}</p>
      <button
        onClick={fetchDocuments}
        className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors duration-150"
      >
        Try Again
      </button>
    </div>
  );

  const renderLoadingState = () => (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
      {[1, 2, 3, 4].map((i) => (
        <div key={i} className="animate-pulse">
          <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
            <div className="h-32 bg-gray-100"></div>
            <div className="p-3">
              <div className="h-4 bg-gray-100 rounded mb-2"></div>
              <div className="h-3 bg-gray-100 rounded w-1/2"></div>
            </div>
          </div>
        </div>
      ))}
    </div>
  );

  return (
    <div className="h-full bg-white overflow-auto">
      <div className="max-w-7xl mx-auto px-6 py-8">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-3xl font-semibold text-gray-900 mb-2">Home</h1>
          </div>
          
          <div className="flex items-center space-x-3">
            <button
              onClick={onNewDocument}
              className="flex items-center px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors duration-150 font-medium text-sm"
            >
              <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
              </svg>
              New Document
            </button>
          </div>
        </div>

        {/* Document Grid */}
        <div className="mb-6">
          <h2 className="text-lg font-medium text-gray-900 mb-6">
            Recent Documents ({documents.length})
          </h2>
          
          {isLoading && renderLoadingState()}
          {error && renderErrorState()}
          {!isLoading && !error && documents.length === 0 && renderEmptyState()}
          {!isLoading && !error && documents.length > 0 && (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
              {documents.map(renderDocumentCard)}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};