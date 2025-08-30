// Custom Node Component for React Flow Graph Visualization
// Displays graph nodes with visual encoding and interactive features

import React, {memo} from 'react';
import {Handle, NodeProps, Position} from 'reactflow';
import {GraphNode} from '../../types/graph';

interface CustomNodeData {
  label: string;
  note: GraphNode;
  isHighlighted?: boolean;
}

export const CustomNode: React.FC<NodeProps<CustomNodeData>> = memo(({ data, selected }) => {
  const { note, isHighlighted } = data;
  
  const nodeSize = Math.max(note.size, 20); // Minimum size of 20px
  const radius = nodeSize;
  
  // Truncate long titles for display
  const displayTitle = note.title.length > 20 
    ? `${note.title.slice(0, 20)}...` 
    : note.title;

  // Determine node border and shadow based on state
  const getBorderStyle = () => {
    if (selected) return '3px solid #1d4ed8'; // Blue border for selected
    if (isHighlighted) return '3px solid #dc2626'; // Red border for search highlights
    return '2px solid #e5e7eb'; // Default gray border
  };

  const getShadowStyle = () => {
    if (selected || isHighlighted) return '0 8px 25px rgba(0, 0, 0, 0.15)';
    return '0 4px 6px rgba(0, 0, 0, 0.1)';
  };

  return (
    <div
      className="relative"
      style={{
        width: radius * 2,
        height: radius * 2,
      }}
    >
      {/* Connection handles */}
      <Handle 
        type="target" 
        position={Position.Top} 
        className="opacity-0 hover:opacity-100 transition-opacity"
        style={{ 
          background: '#3b82f6',
          width: 8,
          height: 8,
          border: 'none',
          top: -4
        }}
      />
      <Handle 
        type="source" 
        position={Position.Bottom} 
        className="opacity-0 hover:opacity-100 transition-opacity"
        style={{ 
          background: '#3b82f6',
          width: 8,
          height: 8,
          border: 'none',
          bottom: -4
        }}
      />

      {/* Main node circle */}
      <div
        className="absolute inset-0 rounded-full flex items-center justify-center cursor-pointer transition-all duration-200 hover:scale-110"
        style={{
          backgroundColor: note.color,
          border: getBorderStyle(),
          boxShadow: getShadowStyle(),
        }}
        title={`${note.title}\nConnections: ${note.connectionCount}\nTags: ${note.tags.join(', ')}`}
      >
        {/* Node content indicator */}
        <div className="text-center">
          {/* Icon based on content type or connection count */}
          {note.connectionCount > 5 ? (
            <svg className="w-4 h-4 mx-auto mb-1 text-gray-700" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M11.3 1.046A1 1 0 0112 2v5h4a1 1 0 01.82 1.573l-7 10A1 1 0 018 18v-5H4a1 1 0 01-.82-1.573l7-10a1 1 0 011.12-.38z" clipRule="evenodd" />
            </svg>
          ) : note.connectionCount > 2 ? (
            <svg className="w-4 h-4 mx-auto mb-1 text-gray-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
          ) : note.tags.length > 0 ? (
            <svg className="w-4 h-4 mx-auto mb-1 text-gray-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
            </svg>
          ) : (
            <svg className="w-4 h-4 mx-auto mb-1 text-gray-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
          )}
          
          {/* Connection count indicator */}
          {note.connectionCount > 0 && (
            <div className="absolute -top-1 -right-1 bg-blue-500 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center font-bold">
              {note.connectionCount > 9 ? '9+' : note.connectionCount}
            </div>
          )}
        </div>
      </div>

      {/* Node label */}
      <div
        className="absolute top-full left-1/2 transform -translate-x-1/2 mt-2 px-2 py-1 bg-white rounded shadow-sm border text-xs text-gray-700 text-center max-w-32 z-10"
        style={{ minWidth: 'max-content' }}
      >
        <div className="font-medium truncate">{displayTitle}</div>
        {note.tags.length > 0 && (
          <div className="text-xs text-gray-500 mt-1 flex flex-wrap gap-1 justify-center">
            {note.tags.slice(0, 2).map((tag, index) => (
              <span 
                key={index}
                className="inline-block bg-gray-100 text-gray-600 px-1 rounded text-xs"
              >
                {tag}
              </span>
            ))}
            {note.tags.length > 2 && (
              <span className="text-gray-400">+{note.tags.length - 2}</span>
            )}
          </div>
        )}
      </div>

      {/* Highlight ring for search results */}
      {isHighlighted && (
        <div
          className="absolute inset-0 rounded-full pointer-events-none"
          style={{
            width: radius * 2 + 8,
            height: radius * 2 + 8,
            left: -4,
            top: -4,
            border: '2px solid #dc2626',
            animation: 'pulse 2s infinite',
          }}
        />
      )}

      {/* Selection ring */}
      {selected && (
        <div
          className="absolute inset-0 rounded-full pointer-events-none"
          style={{
            width: radius * 2 + 12,
            height: radius * 2 + 12,
            left: -6,
            top: -6,
            border: '3px solid #1d4ed8',
            animation: 'ping 1s',
          }}
        />
      )}
    </div>
  );
});

CustomNode.displayName = 'CustomNode';