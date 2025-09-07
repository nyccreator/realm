// Craft-inspired Document Editor Component
// Clean, elegant design with real backend functionality

import React, {useEffect, useRef, useState} from 'react';

interface Note {
  id: string;
  title: string;
  content: string;
  tags: string[];
  status: string;
  createdAt: number[];
  updatedAt: number[];
}

interface CraftDocumentProps {
  documentId: string;
  onTitleChange: (newTitle: string) => void;
  onClose?: () => void;
}

export const CraftDocument: React.FC<CraftDocumentProps> = ({ 
  documentId, 
  onTitleChange,
  onClose
}) => {
  const [note, setNote] = useState<Note | null>(null);
  const [title, setTitle] = useState('Getting Started');
  const [content, setContent] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [lastSaved, setLastSaved] = useState<Date | null>(null);
  const saveTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  // Load document from backend
  useEffect(() => {
    if (documentId && documentId !== 'new') {
      loadDocument();
    } else {
      // New document
      setIsLoading(false);
      setTitle('Getting Started');
      setContent('Hello\n\nWrite something\n\nThis is a new document\n\ngsk_nfyis00vLcAzNiqaPhN7WGdyb3FYShS7GYzmCiJytcAo9B4TUia');
    }
  }, [documentId]);

  // Auto-save functionality
  useEffect(() => {
    if (!isLoading && (title || content)) {
      // Clear existing timeout
      if (saveTimeoutRef.current) {
        clearTimeout(saveTimeoutRef.current);
      }
      
      // Set new timeout for auto-save
      saveTimeoutRef.current = setTimeout(() => {
        saveDocument();
      }, 1000); // Save after 1 second of inactivity
    }
    
    return () => {
      if (saveTimeoutRef.current) {
        clearTimeout(saveTimeoutRef.current);
      }
    };
  }, [title, content]);

  const loadDocument = async () => {
    try {
      setIsLoading(true);
      const response = await fetch(`/api/notes/${documentId}`, {
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
      });

      if (!response.ok) {
        throw new Error('Failed to load document');
      }

      const noteData: Note = await response.json();
      setNote(noteData);
      setTitle(noteData.title);
      setContent(noteData.content);
      onTitleChange(noteData.title);
    } catch (error) {
      console.error('Error loading document:', error);
      // Fallback to default content if load fails
      setTitle('Getting Started');
      setContent('Hello\n\nWrite something\n\nThis is a new document');
    } finally {
      setIsLoading(false);
    }
  };

  const saveDocument = async () => {
    if (!title.trim() && !content.trim()) return;
    
    try {
      setIsSaving(true);
      
      const method = documentId === 'new' ? 'POST' : 'PUT';
      const url = documentId === 'new' ? '/api/notes' : `/api/notes/${documentId}`;
      
      const response = await fetch(url, {
        method,
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          title: title || 'Untitled Document',
          content: content || '',
          tags: []
        })
      });

      if (!response.ok) {
        throw new Error('Failed to save document');
      }

      const savedNote: Note = await response.json();
      setNote(savedNote);
      setLastSaved(new Date());
      
    } catch (error) {
      console.error('Error saving document:', error);
    } finally {
      setIsSaving(false);
    }
  };

  const handleTitleChange = (newTitle: string) => {
    setTitle(newTitle);
    onTitleChange(newTitle);
  };

  const formatContent = (text: string) => {
    return text.split('\n').map((line, index) => (
      <React.Fragment key={index}>
        {line}
        {index < text.split('\n').length - 1 && <br />}
      </React.Fragment>
    ));
  };

  if (isLoading) {
    return (
      <div className="h-full bg-gradient-to-br from-teal-500 via-teal-400 to-emerald-400 flex items-center justify-center">
        <div className="bg-white rounded-lg shadow-lg p-8">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-teal-500 mx-auto"></div>
          <p className="mt-4 text-gray-600 text-center">Loading document...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="h-full bg-gradient-to-br from-teal-500 via-teal-400 to-emerald-400 relative">
      {/* Status Bar */}
      <div className="absolute top-4 right-4 z-10">
        {isSaving && (
          <div className="bg-white/90 backdrop-blur-sm rounded-full px-3 py-1 text-sm text-gray-600">
            Saving...
          </div>
        )}
        {lastSaved && !isSaving && (
          <div className="bg-white/90 backdrop-blur-sm rounded-full px-3 py-1 text-sm text-gray-500">
            Saved {lastSaved.toLocaleTimeString()}
          </div>
        )}
      </div>
      
      {/* Main Document Container */}
      <div className="flex items-center justify-center h-full px-8 py-16">
        <div className="w-full max-w-2xl">
          <div className="bg-white rounded-lg shadow-2xl min-h-[600px] max-h-[80vh] overflow-hidden">
            {/* Document Header */}
            <div className="border-b border-gray-100 px-8 py-6">
              <input
                type="text"
                value={title}
                onChange={(e) => handleTitleChange(e.target.value)}
                placeholder="Document Title"
                className="
                  text-xl font-semibold text-gray-900 bg-transparent border-none outline-none
                  w-full placeholder-gray-400 focus:ring-0 resize-none
                "
                style={{ fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif' }}
              />
            </div>
            
            {/* Document Content */}
            <div className="p-8">
              <textarea
                value={content}
                onChange={(e) => setContent(e.target.value)}
                placeholder="Start writing..."
                className="
                  w-full h-96 resize-none border-none outline-none text-gray-800
                  placeholder-gray-400 text-base leading-relaxed bg-transparent focus:ring-0
                "
                style={{ 
                  fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
                  lineHeight: '1.6'
                }}
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};