// Node Details Panel Component - Shows detailed information about selected nodes
// Displays node content, connections, and provides actions for selected graph nodes

import React, {useState} from 'react';
import {GraphNode} from '../../types/graph';
import {useGraph} from '../../hooks/useGraph';

interface NodeDetailsPanelProps {
  node: GraphNode;
  onClose: () => void;
}

export const NodeDetailsPanel: React.FC<NodeDetailsPanelProps> = ({ node, onClose }) => {
  const { useNodeNeighbors, useSubgraph } = useGraph();
  const [showSubgraph, setShowSubgraph] = useState(false);
  
  // Fetch node neighbors for connection details
  const { 
    data: neighborsData, 
    isLoading: isLoadingNeighbors 
  } = useNodeNeighbors(node.id);

  // Fetch subgraph when requested
  const { 
    data: subgraphData, 
    isLoading: isLoadingSubgraph 
  } = useSubgraph(showSubgraph ? { 
    nodeId: node.id, 
    depth: 2, 
    maxNodes: 20 
  } : null);

  const handleShowSubgraph = () => {
    setShowSubgraph(true);
  };

  const stripHtmlTags = (html: string): string => {
    const temp = document.createElement('div');
    temp.innerHTML = html;
    return temp.textContent || temp.innerText || '';
  };

  const truncateContent = (content: string, maxLength: number = 200): string => {
    const plainText = stripHtmlTags(content);
    return plainText.length > maxLength 
      ? plainText.substring(0, maxLength) + '...' 
      : plainText;
  };

  const formatDate = (dateString?: string): string => {
    if (!dateString) return 'Unknown';
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  return (
    <div className="fixed right-0 top-0 h-full w-96 bg-white shadow-xl border-l border-gray-200 z-50 overflow-y-auto">
      <div className="p-6">
        {/* Header */}
        <div className="flex items-start justify-between mb-6">
          <div className="flex-1 min-w-0">
            <h2 className="text-xl font-bold text-gray-900 break-words">
              {node.title}
            </h2>
          </div>
          <button
            onClick={onClose}
            className="flex-shrink-0 ml-4 p-2 text-gray-400 hover:text-gray-600 rounded-md hover:bg-gray-100 transition-colors"
            title="Close panel"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Node Visual Indicator */}
        <div className="flex items-center space-x-3 mb-6">
          <div
            className="w-8 h-8 rounded-full border-2 border-gray-300"
            style={{ backgroundColor: node.color }}
            title={`Node color: ${node.color}`}
          />
          <div className="flex-1">
            <div className="text-sm text-gray-600">
              Size: {node.size} • Connections: {node.connectionCount}
            </div>
          </div>
        </div>

        {/* Tags */}
        {node.tags && node.tags.length > 0 && (
          <div className="mb-6">
            <h3 className="text-sm font-medium text-gray-700 mb-2">Tags</h3>
            <div className="flex flex-wrap gap-2">
              {node.tags.map((tag, index) => (
                <span
                  key={index}
                  className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800"
                >
                  {tag}
                </span>
              ))}
            </div>
          </div>
        )}

        {/* Content Preview */}
        <div className="mb-6">
          <h3 className="text-sm font-medium text-gray-700 mb-2">Content Preview</h3>
          <div className="bg-gray-50 rounded-md p-3 text-sm text-gray-700 border">
            {node.content ? (
              <p>{truncateContent(node.content)}</p>
            ) : (
              <p className="text-gray-500 italic">No content available</p>
            )}
          </div>
        </div>

        {/* Timestamps */}
        <div className="mb-6">
          <h3 className="text-sm font-medium text-gray-700 mb-2">Timeline</h3>
          <div className="space-y-2 text-sm">
            {node.createdAt && (
              <div className="flex items-center space-x-2">
                <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                </svg>
                <span className="text-gray-600">Created: {formatDate(node.createdAt)}</span>
              </div>
            )}
            {node.updatedAt && (
              <div className="flex items-center space-x-2">
                <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                </svg>
                <span className="text-gray-600">Updated: {formatDate(node.updatedAt)}</span>
              </div>
            )}
          </div>
        </div>

        {/* Connection Information */}
        <div className="mb-6">
          <h3 className="text-sm font-medium text-gray-700 mb-2">
            Connections ({node.connectionCount})
          </h3>
          
          {isLoadingNeighbors ? (
            <div className="flex items-center space-x-2 text-sm text-gray-500">
              <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              <span>Loading connections...</span>
            </div>
          ) : neighborsData && neighborsData.nodes.length > 1 ? (
            <div className="space-y-2">
              {neighborsData.nodes
                .filter(n => n.id !== node.id)
                .slice(0, 5)
                .map((neighbor) => (
                  <div key={neighbor.id} className="flex items-center space-x-3 p-2 bg-gray-50 rounded-md">
                    <div
                      className="w-3 h-3 rounded-full flex-shrink-0"
                      style={{ backgroundColor: neighbor.color }}
                    />
                    <div className="flex-1 min-w-0">
                      <div className="text-sm font-medium text-gray-900 truncate">
                        {neighbor.title}
                      </div>
                      {neighbor.tags.length > 0 && (
                        <div className="text-xs text-gray-500">
                          {neighbor.tags.slice(0, 2).join(', ')}
                        </div>
                      )}
                    </div>
                  </div>
                ))}
              
              {neighborsData.nodes.length > 6 && (
                <div className="text-xs text-gray-500 text-center">
                  +{neighborsData.nodes.length - 6} more connections
                </div>
              )}
            </div>
          ) : (
            <div className="text-sm text-gray-500 italic">
              No connections found
            </div>
          )}
        </div>

        {/* Actions */}
        <div className="border-t border-gray-200 pt-6">
          <h3 className="text-sm font-medium text-gray-700 mb-3">Actions</h3>
          <div className="space-y-3">
            <button
              onClick={handleShowSubgraph}
              disabled={isLoadingSubgraph}
              className="w-full flex items-center justify-center space-x-2 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors"
            >
              {isLoadingSubgraph ? (
                <>
                  <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  <span>Loading...</span>
                </>
              ) : (
                <>
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                  <span>Focus Subgraph</span>
                </>
              )}
            </button>

            {/* Edit Note Button - This would integrate with the note editor */}
            <button className="w-full flex items-center justify-center space-x-2 px-4 py-2 border border-gray-300 text-gray-700 rounded-md hover:bg-gray-50 transition-colors">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
              <span>Edit Note</span>
            </button>
          </div>
        </div>

        {/* Subgraph Results */}
        {subgraphData && showSubgraph && (
          <div className="border-t border-gray-200 pt-6 mt-6">
            <h3 className="text-sm font-medium text-gray-700 mb-3">
              Subgraph ({subgraphData.nodes.length} nodes, {subgraphData.edges.length} edges)
            </h3>
            <div className="text-sm text-gray-600 bg-blue-50 p-3 rounded-md">
              Found {subgraphData.nodes.length - 1} connected nodes within 2 degrees of separation.
              The graph view has been updated to highlight this subgraph.
            </div>
          </div>
        )}
      </div>
    </div>
  );
};