// Modern Graph Visualization with Craft-inspired design
// Clean, minimalist graph with document preview functionality

import React, {useCallback, useEffect, useMemo, useState} from 'react';
import ReactFlow, {
    Background,
    BackgroundVariant,
    ConnectionLineType,
    Controls,
    Edge,
    Handle,
    MarkerType,
    MiniMap,
    Node,
    NodeProps,
    Position,
    ReactFlowProvider,
    useEdgesState,
    useNodesState,
} from 'reactflow';
import {AnimatePresence, motion} from 'framer-motion';
import {Calendar, Eye, EyeOff, FileText, Link2, Maximize2, RotateCcw, Search, Tag} from 'lucide-react';
import 'reactflow/dist/style.css';

import {GraphData} from '../types/graph';
import {useGraph} from '../hooks/useGraph';
import {Note} from '../types/note';

// Custom node component with Craft-inspired design
const CraftNode: React.FC<NodeProps> = ({ data, selected }) => {
  const [isHovered, setIsHovered] = useState(false);
  
  return (
    <motion.div
      className="relative group"
      initial={{ opacity: 0, scale: 0.8 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ type: "spring", stiffness: 300, damping: 25 }}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
    >
      <Handle
        type="target"
        position={Position.Top}
        className="w-2 h-2 bg-blue-500 border-2 border-white opacity-0 group-hover:opacity-100 transition-opacity"
      />
      
      <div
        className={`
          relative bg-white rounded-xl shadow-sm border-2 p-4 min-w-[180px] max-w-[220px]
          transition-all duration-200 cursor-pointer
          ${selected 
            ? 'border-blue-500 shadow-lg scale-105' 
            : isHovered
              ? 'border-gray-300 shadow-md scale-102'
              : 'border-gray-200 hover:border-gray-300'
          }
        `}
      >
        {/* Node Header */}
        <div className="flex items-start space-x-2 mb-2">
          <div className="flex-shrink-0 w-6 h-6 bg-blue-50 rounded-md flex items-center justify-center">
            <FileText className="h-3 w-3 text-blue-600" />
          </div>
          <h3 className="font-semibold text-gray-900 text-sm leading-tight truncate flex-1">
            {data.title || 'Untitled'}
          </h3>
        </div>

        {/* Node Preview */}
        {data.content && (
          <p className="text-xs text-gray-600 leading-relaxed mb-3 line-clamp-2">
            {data.content.replace(/<[^>]*>/g, '').substring(0, 80)}...
          </p>
        )}

        {/* Node Meta */}
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            {data.tags && data.tags.length > 0 && (
              <div className="flex items-center space-x-1">
                <Tag className="h-3 w-3 text-gray-400" />
                <span className="text-xs text-gray-500">
                  {data.tags.length}
                </span>
              </div>
            )}
            
            {data.updatedAt && (
              <div className="flex items-center space-x-1">
                <Calendar className="h-3 w-3 text-gray-400" />
                <span className="text-xs text-gray-500">
                  {new Date(data.updatedAt).toLocaleDateString('en-US', { 
                    month: 'short', 
                    day: 'numeric' 
                  })}
                </span>
              </div>
            )}
          </div>
          
          {data.connections > 0 && (
            <div className="flex items-center space-x-1">
              <Link2 className="h-3 w-3 text-blue-500" />
              <span className="text-xs text-blue-600 font-medium">
                {data.connections}
              </span>
            </div>
          )}
        </div>

        {/* Expand indicator */}
        <motion.div
          className="absolute top-2 right-2 opacity-0 group-hover:opacity-100"
          initial={{ scale: 0 }}
          animate={{ scale: isHovered ? 1 : 0 }}
          transition={{ duration: 0.2 }}
        >
          <div className="w-5 h-5 bg-gray-100 rounded flex items-center justify-center">
            <Maximize2 className="h-3 w-3 text-gray-600" />
          </div>
        </motion.div>
      </div>

      <Handle
        type="source"
        position={Position.Bottom}
        className="w-2 h-2 bg-blue-500 border-2 border-white opacity-0 group-hover:opacity-100 transition-opacity"
      />
    </motion.div>
  );
};

// Document preview tooltip
const DocumentPreview: React.FC<{ 
  node: any; 
  position: { x: number; y: number }; 
  onClose: () => void;
}> = ({ node, position, onClose }) => {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.9, y: 10 }}
      animate={{ opacity: 1, scale: 1, y: 0 }}
      exit={{ opacity: 0, scale: 0.9, y: 10 }}
      className="fixed z-50 bg-white rounded-xl shadow-xl border border-gray-200 p-4 max-w-sm"
      style={{
        left: position.x + 10,
        top: position.y - 100,
        maxHeight: '300px',
        overflow: 'auto'
      }}
      onMouseLeave={onClose}
    >
      <div className="flex items-start space-x-3 mb-3">
        <div className="w-8 h-8 bg-blue-50 rounded-lg flex items-center justify-center">
          <FileText className="h-4 w-4 text-blue-600" />
        </div>
        <div className="flex-1 min-w-0">
          <h3 className="font-semibold text-gray-900 mb-1">{node.title}</h3>
          {node.tags && node.tags.length > 0 && (
            <div className="flex flex-wrap gap-1 mb-2">
              {node.tags.slice(0, 3).map((tag: string, index: number) => (
                <span
                  key={index}
                  className="px-2 py-0.5 text-xs bg-gray-100 text-gray-600 rounded-full"
                >
                  #{tag}
                </span>
              ))}
            </div>
          )}
        </div>
      </div>
      
      {node.content && (
        <div className="text-sm text-gray-700 leading-relaxed">
          {node.content.replace(/<[^>]*>/g, '').substring(0, 200)}
          {node.content.length > 200 && '...'}
        </div>
      )}
      
      <div className="flex items-center justify-between mt-3 pt-3 border-t border-gray-100">
        <span className="text-xs text-gray-500">
          Updated {new Date(node.updatedAt).toLocaleDateString()}
        </span>
        <button className="text-xs text-blue-600 hover:text-blue-700 font-medium">
          Open →
        </button>
      </div>
    </motion.div>
  );
};

const nodeTypes = {
  craftNode: CraftNode,
};

interface ModernGraphViewProps {
  className?: string;
  onNodeSelect?: (node: Note) => void;
}

export const ModernGraphView: React.FC<ModernGraphViewProps> = ({ 
  className,
  onNodeSelect
}) => {
  const { useGraphData } = useGraph();
  
  // Handle case where QueryClient might not be available
  let data: GraphData | null | undefined = null;
  let isLoading: boolean = false;
  let error: string | null = null;
  let refetch: () => void = () => {};
  
  try {
    const queryResult = useGraphData();
    data = queryResult.data;
    isLoading = queryResult.isLoading;
    error = queryResult.error ? String(queryResult.error) : null;
    refetch = queryResult.refetch;
  } catch (queryError) {
    data = null;
    isLoading = false;
    error = 'Graph data unavailable';
    refetch = () => {};
  }
  
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [showMiniMap, setShowMiniMap] = useState(false);
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
  const [previewNode, setPreviewNode] = useState<{ node: any; position: { x: number; y: number } } | null>(null);

  // Convert graph data to React Flow format with better layout
  const convertToFlowFormat = useCallback((graphData: GraphData) => {
    if (!graphData?.nodes || !graphData?.edges) {
      return { flowNodes: [], flowEdges: [] };
    }

    // Enhanced circular layout for better visualization
    const centerX = 400;
    const centerY = 300;
    const radius = Math.max(150, Math.min(300, graphData.nodes.length * 20));
    
    const flowNodes: Node[] = graphData.nodes.map((node, index) => {
      const angle = (2 * Math.PI * index) / graphData.nodes.length;
      const x = centerX + radius * Math.cos(angle);
      const y = centerY + radius * Math.sin(angle);
      
      // Count connections for this node
      const connections = graphData.edges.filter(
        edge => edge.source === node.id || edge.target === node.id
      ).length;
      
      return {
        id: node.id,
        type: 'craftNode',
        position: { x, y },
        data: {
          title: node.title,
          content: node.content,
          tags: node.tags,
          updatedAt: node.updatedAt || new Date().toISOString(),
          connections,
          selected: node.id === selectedNodeId,
        },
      };
    });

    const flowEdges: Edge[] = graphData.edges.map((edge) => ({
      id: edge.id,
      source: edge.source,
      target: edge.target,
      type: ConnectionLineType.SmoothStep,
      animated: false,
      style: {
        strokeWidth: 2,
        stroke: '#e5e7eb',
        strokeDasharray: edge.type === 'weak' ? '5,5' : undefined,
      },
      markerEnd: {
        type: MarkerType.ArrowClosed,
        color: '#9ca3af',
        width: 12,
        height: 12,
      },
      label: edge.context || '',
      labelStyle: {
        fontSize: '11px',
        color: '#6b7280',
        backgroundColor: 'rgba(255, 255, 255, 0.8)',
        borderRadius: '4px',
        padding: '2px 6px',
      },
    }));

    return { flowNodes, flowEdges };
  }, [selectedNodeId]);

  // Update nodes and edges when data changes
  useEffect(() => {
    if (data) {
      const { flowNodes, flowEdges } = convertToFlowFormat(data);
      setNodes(flowNodes);
      setEdges(flowEdges);
    }
  }, [data, convertToFlowFormat, setNodes, setEdges]);

  // Filter data based on search
  const filteredNodes = useMemo(() => {
    if (!searchQuery || !data) return nodes;
    
    return nodes.filter(node =>
      node.data.title?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      node.data.tags?.some((tag: string) => tag.toLowerCase().includes(searchQuery.toLowerCase()))
    );
  }, [nodes, searchQuery, data]);

  const handleNodeClick = useCallback((event: React.MouseEvent, node: Node) => {
    setSelectedNodeId(node.id);
    if (onNodeSelect) {
      // Convert back to Note format if needed
      const noteData = data?.nodes.find(n => n.id === node.id);
      if (noteData) {
        // Create a Note-compatible object
        const note: Note = {
          id: noteData.id,
          title: noteData.title,
          content: noteData.content,
          tags: noteData.tags || [],
          createdAt: noteData.createdAt || new Date().toISOString(),
          updatedAt: noteData.updatedAt || new Date().toISOString(),
          createdBy: {
            id: 'current-user',
            displayName: 'Current User',
            email: 'user@example.com'
          }
        };
        onNodeSelect(note);
      }
    }
  }, [onNodeSelect, data]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-full bg-gray-50">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading graph...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center h-full bg-gray-50 p-8">
        <div className="text-center">
          <div className="w-16 h-16 bg-red-50 rounded-full flex items-center justify-center mb-4">
            <FileText className="h-8 w-8 text-red-500" />
          </div>
          <h3 className="text-lg font-medium text-gray-900 mb-2">Unable to load graph</h3>
          <p className="text-gray-600 mb-4">{error}</p>
          <button
            onClick={refetch}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            Try Again
          </button>
        </div>
      </div>
    );
  }

  if (!data || data.nodes.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center h-full bg-gray-50 p-8">
        <div className="text-center max-w-md">
          <div className="w-16 h-16 bg-blue-50 rounded-full flex items-center justify-center mb-4">
            <FileText className="h-8 w-8 text-blue-500" />
          </div>
          <h3 className="text-lg font-medium text-gray-900 mb-2">No documents to visualize</h3>
          <p className="text-gray-600">
            Create some documents and link them together to see your knowledge graph come to life.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className={`h-full relative bg-gray-50 ${className}`}>
      {/* Controls */}
      <motion.div
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        className="absolute top-4 left-4 right-4 z-10"
      >
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
              <input
                type="text"
                placeholder="Search documents..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="
                  pl-10 pr-4 py-2 bg-white border border-gray-200 rounded-lg
                  focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500
                  text-sm min-w-[250px] shadow-sm
                "
              />
            </div>
            
            <div className="flex items-center bg-white border border-gray-200 rounded-lg shadow-sm">
              <button
                onClick={() => setShowMiniMap(!showMiniMap)}
                className={`
                  p-2 rounded-l-lg transition-colors text-sm
                  ${showMiniMap 
                    ? 'bg-blue-50 text-blue-600' 
                    : 'text-gray-600 hover:text-gray-900'
                  }
                `}
              >
                {showMiniMap ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              </button>
              
              <button
                onClick={refetch}
                className="p-2 text-gray-600 hover:text-gray-900 transition-colors border-l border-gray-200"
              >
                <RotateCcw className="h-4 w-4" />
              </button>
            </div>
          </div>
          
          <div className="bg-white px-3 py-2 rounded-lg border border-gray-200 shadow-sm">
            <div className="flex items-center space-x-3 text-sm text-gray-600">
              <span>{data.nodes.length} documents</span>
              <span>•</span>
              <span>{data.edges.length} connections</span>
            </div>
          </div>
        </div>
      </motion.div>

      {/* Graph */}
      <ReactFlow
        nodes={filteredNodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onNodeClick={handleNodeClick}
        nodeTypes={nodeTypes}
        connectionLineType={ConnectionLineType.SmoothStep}
        defaultEdgeOptions={{
          animated: false,
          style: { strokeWidth: 2, stroke: '#e5e7eb' },
        }}
        fitView
        fitViewOptions={{
          padding: 0.2,
          includeHiddenNodes: false,
        }}
        minZoom={0.2}
        maxZoom={2}
        className="bg-gray-50"
      >
        <Background 
          variant={BackgroundVariant.Dots} 
          gap={24} 
          size={1}
          color="#f3f4f6"
        />
        
        <Controls 
          className="bg-white shadow-lg border border-gray-200 rounded-lg"
          showInteractive={false}
        />
        
        {showMiniMap && (
          <MiniMap
            className="bg-white shadow-lg border border-gray-200 rounded-lg"
            nodeColor="#3b82f6"
            nodeStrokeWidth={2}
            maskColor="rgba(243, 244, 246, 0.8)"
            style={{
              backgroundColor: 'white',
            }}
          />
        )}
      </ReactFlow>

      {/* Document Preview Tooltip */}
      <AnimatePresence>
        {previewNode && (
          <DocumentPreview
            node={previewNode.node}
            position={previewNode.position}
            onClose={() => setPreviewNode(null)}
          />
        )}
      </AnimatePresence>
    </div>
  );
};

// Wrapper component with provider
export const ModernGraphViewWithProvider: React.FC<ModernGraphViewProps> = (props) => {
  return (
    <ReactFlowProvider>
      <ModernGraphView {...props} />
    </ReactFlowProvider>
  );
};

export default ModernGraphViewWithProvider;