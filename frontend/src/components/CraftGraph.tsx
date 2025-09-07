// Craft-inspired Graph Visualization Component
// Clean, modern graph view with document previews

import React from 'react';

interface CraftGraphProps {
  onDocumentOpen: (documentId: string, title: string) => void;
}

export const CraftGraph: React.FC<CraftGraphProps> = ({ onDocumentOpen }) => {
  return (
    <div className="h-full bg-gray-50 flex items-center justify-center">
      <div className="text-center max-w-md">
        <div className="w-16 h-16 bg-purple-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <svg className="w-8 h-8 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 12a9 9 0 01-9 9m9-9a9 9 0 00-9-9m9 9H3m9 9a9 9 0 01-9-9m9 9c1.657 0 3-4.03 3-9s-1.343-9-3-9m0 18c-1.657 0-3-4.03-3-9s1.343-9 3-9m-9 9a9 9 0 019-9" />
          </svg>
        </div>
        <h2 className="text-xl font-semibold text-gray-900 mb-2">Knowledge Graph</h2>
        <p className="text-gray-600 mb-4">
          Visualize the connections between your documents and ideas. 
          The graph view will be available once you create some documents.
        </p>
        <div className="text-sm text-gray-500">
          Coming soon: Interactive graph visualization with React Flow
        </div>
      </div>
    </div>
  );
};