// Graph Legend Component - Visual encoding explanation
// Shows users what the different visual elements in the graph represent

import React from 'react';

interface GraphLegendProps {
  onClose: () => void;
}

export const GraphLegend: React.FC<GraphLegendProps> = ({ onClose }) => {
  return (
    <div className="bg-white rounded-lg shadow-lg p-4 max-w-sm border">
      <div className="flex items-center justify-between mb-3">
        <h3 className="font-semibold text-gray-900">Graph Legend</h3>
        <button
          onClick={onClose}
          className="text-gray-400 hover:text-gray-600 p-1"
          title="Close legend"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <div className="space-y-4 text-sm">
        {/* Node Size Legend */}
        <div>
          <h4 className="font-medium text-gray-700 mb-2">Node Size</h4>
          <div className="space-y-2">
            <div className="flex items-center space-x-3">
              <div className="w-6 h-6 bg-blue-200 rounded-full border-2 border-gray-300"></div>
              <span className="text-gray-600">Small (little content)</span>
            </div>
            <div className="flex items-center space-x-3">
              <div className="w-8 h-8 bg-blue-200 rounded-full border-2 border-gray-300"></div>
              <span className="text-gray-600">Medium (moderate content)</span>
            </div>
            <div className="flex items-center space-x-3">
              <div className="w-10 h-10 bg-blue-200 rounded-full border-2 border-gray-300"></div>
              <span className="text-gray-600">Large (rich content)</span>
            </div>
          </div>
        </div>

        {/* Node Color Legend */}
        <div>
          <h4 className="font-medium text-gray-700 mb-2">Node Color (Recency)</h4>
          <div className="space-y-2">
            <div className="flex items-center space-x-3">
              <div className="w-6 h-6 bg-green-400 rounded-full border-2 border-gray-300"></div>
              <span className="text-gray-600">Recent (&lt; 1 week)</span>
            </div>
            <div className="flex items-center space-x-3">
              <div className="w-6 h-6 bg-blue-400 rounded-full border-2 border-gray-300"></div>
              <span className="text-gray-600">Medium (&lt; 1 month)</span>
            </div>
            <div className="flex items-center space-x-3">
              <div className="w-6 h-6 bg-gray-400 rounded-full border-2 border-gray-300"></div>
              <span className="text-gray-600">Older (&gt; 1 month)</span>
            </div>
            <div className="flex items-center space-x-3">
              <div className="w-6 h-6 bg-purple-400 rounded-full border-2 border-gray-300"></div>
              <span className="text-gray-600">Tagged notes</span>
            </div>
          </div>
        </div>

        {/* Connection Indicators */}
        <div>
          <h4 className="font-medium text-gray-700 mb-2">Connections</h4>
          <div className="space-y-2">
            <div className="flex items-center space-x-3">
              <div className="relative w-6 h-6 bg-blue-200 rounded-full border-2 border-gray-300">
                <div className="absolute -top-1 -right-1 bg-blue-500 text-white text-xs rounded-full w-4 h-4 flex items-center justify-center font-bold">
                  3
                </div>
              </div>
              <span className="text-gray-600">Connection count badge</span>
            </div>
            <div className="flex items-center space-x-3">
              <svg className="w-6 h-6 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
              <span className="text-gray-600">Highly connected (hub)</span>
            </div>
          </div>
        </div>

        {/* Edge Legend */}
        <div>
          <h4 className="font-medium text-gray-700 mb-2">Connections</h4>
          <div className="space-y-2">
            <div className="flex items-center space-x-3">
              <div className="w-8 h-px bg-gray-400"></div>
              <span className="text-gray-600">Normal link</span>
            </div>
            <div className="flex items-center space-x-3">
              <div className="w-8 h-px bg-blue-500" style={{ height: '2px' }}></div>
              <span className="text-gray-600">Strong connection</span>
            </div>
            <div className="flex items-center space-x-3">
              <div className="w-8 h-px bg-blue-500 animate-pulse" style={{ height: '2px' }}></div>
              <span className="text-gray-600">Search highlight</span>
            </div>
          </div>
        </div>

        {/* Selection States */}
        <div>
          <h4 className="font-medium text-gray-700 mb-2">Selection States</h4>
          <div className="space-y-2">
            <div className="flex items-center space-x-3">
              <div className="w-6 h-6 bg-blue-200 rounded-full border-2 border-blue-600"></div>
              <span className="text-gray-600">Selected node</span>
            </div>
            <div className="flex items-center space-x-3">
              <div className="w-6 h-6 bg-blue-200 rounded-full border-2 border-red-600"></div>
              <span className="text-gray-600">Search result</span>
            </div>
          </div>
        </div>
      </div>

      {/* Instructions */}
      <div className="mt-4 pt-3 border-t border-gray-200">
        <h4 className="font-medium text-gray-700 mb-2 text-xs">Interactions</h4>
        <div className="text-xs text-gray-600 space-y-1">
          <div>• Click nodes to view details</div>
          <div>• Drag nodes to reposition</div>
          <div>• Use mouse wheel to zoom</div>
          <div>• Hold Shift + click to select multiple</div>
          <div>• Use search to highlight nodes</div>
        </div>
      </div>
    </div>
  );
};