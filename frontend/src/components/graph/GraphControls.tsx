// Graph Controls Component - Status information and control buttons
// Displays graph statistics and provides quick access to graph controls

import React from 'react';

interface GraphControlsProps {
  nodeCount: number;
  edgeCount: number;
  onShowLegend: () => void;
}

export const GraphControls: React.FC<GraphControlsProps> = ({
  nodeCount,
  edgeCount,
  onShowLegend
}) => {
  return (
    <div className="bg-white rounded-lg shadow-md p-3 border min-w-48">
      <div className="space-y-3">
        {/* Graph Statistics */}
        <div>
          <h4 className="text-sm font-medium text-gray-700 mb-2">Graph Stats</h4>
          <div className="space-y-1 text-sm">
            <div className="flex items-center justify-between">
              <span className="text-gray-600">Notes:</span>
              <span className="font-medium text-gray-900">{nodeCount}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-gray-600">Links:</span>
              <span className="font-medium text-gray-900">{edgeCount}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-gray-600">Density:</span>
              <span className="font-medium text-gray-900">
                {nodeCount > 1 
                  ? ((edgeCount / (nodeCount * (nodeCount - 1))) * 100).toFixed(1) + '%'
                  : '0%'
                }
              </span>
            </div>
          </div>
        </div>

        {/* Quick Actions */}
        <div className="border-t border-gray-200 pt-3">
          <h4 className="text-sm font-medium text-gray-700 mb-2">Quick Actions</h4>
          <div className="space-y-2">
            <button
              onClick={onShowLegend}
              className="w-full flex items-center justify-center space-x-2 px-3 py-2 text-sm bg-gray-100 hover:bg-gray-200 rounded-md transition-colors"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <span>Show Legend</span>
            </button>
          </div>
        </div>

        {/* Graph Health Indicators */}
        <div className="border-t border-gray-200 pt-3">
          <h4 className="text-sm font-medium text-gray-700 mb-2">Graph Health</h4>
          <div className="space-y-2">
            {/* Connectivity */}
            <div className="flex items-center space-x-2">
              <div className={`w-3 h-3 rounded-full ${
                edgeCount > 0 ? 'bg-green-400' : 'bg-gray-300'
              }`}></div>
              <span className="text-sm text-gray-600">
                {edgeCount > 0 ? 'Connected' : 'Isolated notes'}
              </span>
            </div>

            {/* Size indicator */}
            <div className="flex items-center space-x-2">
              <div className={`w-3 h-3 rounded-full ${
                nodeCount > 10 ? 'bg-blue-400' : nodeCount > 3 ? 'bg-yellow-400' : 'bg-orange-400'
              }`}></div>
              <span className="text-sm text-gray-600">
                {nodeCount > 10 ? 'Rich knowledge base' : 
                 nodeCount > 3 ? 'Growing collection' : 'Starting out'}
              </span>
            </div>

            {/* Activity indicator */}
            <div className="flex items-center space-x-2">
              <div className="w-3 h-3 rounded-full bg-green-400 animate-pulse"></div>
              <span className="text-sm text-gray-600">
                Live updates
              </span>
            </div>
          </div>
        </div>

        {/* Performance Info */}
        {nodeCount > 50 && (
          <div className="border-t border-gray-200 pt-3">
            <div className="flex items-center space-x-2 text-xs text-amber-600">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.728-.833-2.498 0L4.316 15.5c-.77.833.192 2.5 1.732 2.5z" />
              </svg>
              <span>Large graph - consider filtering</span>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};