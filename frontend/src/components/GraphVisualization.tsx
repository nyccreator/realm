// Graph Visualization Component for Section 3.3 - Graph Visualization
// Interactive graph visualization using React Flow with pan, zoom, and node interactions

import React, {useCallback, useEffect, useMemo, useState} from 'react';
import ReactFlow, {
    addEdge,
    Background,
    BackgroundVariant,
    Connection,
    Controls,
    Edge,
    MiniMap,
    Node,
    NodeTypes,
    OnConnect,
    Panel,
    ReactFlowInstance,
    useEdgesState,
    useNodesState,
} from 'reactflow';
import 'reactflow/dist/style.css';

import {GraphData, GraphEdge, GraphNode} from '../types/graph';
import {useGraph} from '../hooks/useGraph';
import {LoadingSpinner} from './LoadingSpinner';
import {CustomNode} from './graph/CustomNode';
import {GraphLegend} from './graph/GraphLegend';
import {GraphControls} from './graph/GraphControls';
import {GraphSearch} from './graph/GraphSearch';
import {NodeDetailsPanel} from './graph/NodeDetailsPanel';

// Define custom node types
const nodeTypes: NodeTypes = {
  custom: CustomNode,
};

// Graph layout algorithm using force-directed positioning
const calculateLayout = (nodes: GraphNode[], edges: GraphEdge[]) => {
  // Simple force-directed layout - can be enhanced with D3 later
  const width = 1000;
  const height = 800;
  const centerX = width / 2;
  const centerY = height / 2;
  
  // If no nodes, return empty
  if (nodes.length === 0) return [];

  // Single node positioning
  if (nodes.length === 1) {
    return [{
      ...nodes[0],
      x: centerX,
      y: centerY
    }];
  }

  // Multiple nodes - circular layout as a starting point
  const radius = Math.min(width, height) * 0.3;
  const angleStep = (2 * Math.PI) / nodes.length;
  
  return nodes.map((node, index) => ({
    ...node,
    x: centerX + radius * Math.cos(index * angleStep),
    y: centerY + radius * Math.sin(index * angleStep)
  }));
};

// Convert graph data to React Flow format
const convertToReactFlow = (graphData: GraphData) => {
  const positionedNodes = calculateLayout(graphData.nodes, graphData.edges);
  
  const reactFlowNodes: Node[] = positionedNodes.map((node) => ({
    id: node.id,
    position: { x: node.x || 0, y: node.y || 0 },
    data: {
      label: node.title,
      note: node,
      isHighlighted: node.highlighted,
    },
    type: 'custom',
    style: {
      backgroundColor: node.color,
      border: node.highlighted ? '3px solid #3b82f6' : '2px solid #e5e7eb',
      borderRadius: '50%',
      width: node.size * 2,
      height: node.size * 2,
      fontSize: '12px',
      color: '#1f2937',
    },
  }));

  const reactFlowEdges: Edge[] = graphData.edges.map((edge) => ({
    id: edge.id,
    source: edge.source,
    target: edge.target,
    type: 'smoothstep',
    animated: edge.highlighted || false,
    style: {
      stroke: edge.highlighted ? '#3b82f6' : '#9ca3af',
      strokeWidth: edge.highlighted ? 3 : 2,
    },
    data: {
      relationship: edge,
    },
  }));

  return { nodes: reactFlowNodes, edges: reactFlowEdges };
};

export const GraphVisualization: React.FC = () => {
  const {
    useGraphData,
    useSearchNodes,
    updateSelectedNode,
    searchQuery,
    updateSearchQuery,
  } = useGraph();

  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const [reactFlowInstance, setReactFlowInstance] = useState<ReactFlowInstance | null>(null);
  const [showSearch, setShowSearch] = useState(false);
  const [showLegend, setShowLegend] = useState(true);
  const [selectedNode, setSelectedNode] = useState<GraphNode | null>(null);

  // Fetch main graph data
  const { 
    data: graphData, 
    isLoading, 
    error,
    refetch
  } = useGraphData({ maxNodes: 100 });

  // Search functionality
  const { 
    data: searchResults, 
    isLoading: isSearching 
  } = useSearchNodes(searchQuery, searchQuery.length > 2);

  // Convert graph data to React Flow format when data changes
  useEffect(() => {
    if (graphData) {
      const { nodes: flowNodes, edges: flowEdges } = convertToReactFlow(graphData);
      setNodes(flowNodes);
      setEdges(flowEdges);
    }
  }, [graphData, setNodes, setEdges]);

  // Handle search results highlighting
  useEffect(() => {
    if (searchResults && searchQuery) {
      setNodes((prevNodes) =>
        prevNodes.map((node) => {
          const isHighlighted = searchResults.some(result => result.id === node.id);
          return {
            ...node,
            data: {
              ...node.data,
              isHighlighted,
            },
            style: {
              ...node.style,
              border: isHighlighted ? '3px solid #3b82f6' : '2px solid #e5e7eb',
            },
          };
        })
      );

      setEdges((prevEdges) =>
        prevEdges.map((edge) => {
          const isHighlighted = searchResults.some(result => 
            result.id === edge.source || result.id === edge.target
          );
          return {
            ...edge,
            animated: isHighlighted,
            style: {
              ...edge.style,
              stroke: isHighlighted ? '#3b82f6' : '#9ca3af',
              strokeWidth: isHighlighted ? 3 : 2,
            },
          };
        })
      );
    } else {
      // Reset highlighting when search is cleared
      setNodes((prevNodes) =>
        prevNodes.map((node) => ({
          ...node,
          data: {
            ...node.data,
            isHighlighted: false,
          },
          style: {
            ...node.style,
            border: '2px solid #e5e7eb',
          },
        }))
      );

      setEdges((prevEdges) =>
        prevEdges.map((edge) => ({
          ...edge,
          animated: false,
          style: {
            ...edge.style,
            stroke: '#9ca3af',
            strokeWidth: 2,
          },
        }))
      );
    }
  }, [searchResults, searchQuery, setNodes, setEdges]);

  const onConnect: OnConnect = useCallback(
    (params: Connection) => setEdges((eds) => addEdge(params, eds)),
    [setEdges]
  );

  const onInit = useCallback((instance: ReactFlowInstance) => {
    setReactFlowInstance(instance);
  }, []);

  const onNodeClick = useCallback((event: React.MouseEvent, node: Node) => {
    const graphNode = node.data.note as GraphNode;
    setSelectedNode(graphNode);
    updateSelectedNode(graphNode.id);
    
    // Center the view on the clicked node
    if (reactFlowInstance) {
      const { zoom } = reactFlowInstance.getViewport();
      reactFlowInstance.setCenter(node.position.x, node.position.y, { zoom, duration: 800 });
    }
  }, [reactFlowInstance, updateSelectedNode]);

  const onSearchQueryChange = useCallback((query: string) => {
    updateSearchQuery(query);
  }, [updateSearchQuery]);

  const handleFitView = useCallback(() => {
    if (reactFlowInstance) {
      reactFlowInstance.fitView({ duration: 800, padding: 0.1 });
    }
  }, [reactFlowInstance]);

  const handleRefresh = useCallback(() => {
    refetch();
  }, [refetch]);

  const miniMapStyle = useMemo(() => ({
    backgroundColor: '#f8fafc',
    maskColor: 'rgba(0, 0, 0, 0.1)',
  }), []);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-full">
        <LoadingSpinner size="xl" text="Loading graph visualization..." />
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center h-full space-y-4">
        <div className="text-red-600 text-center">
          <h3 className="text-lg font-medium mb-2">Failed to load graph</h3>
          <p className="text-sm text-gray-600">{(error as Error).message}</p>
        </div>
        <button
          onClick={handleRefresh}
          className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
        >
          Retry
        </button>
      </div>
    );
  }

  return (
    <div className="h-full w-full relative bg-gray-50">
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        onInit={onInit}
        onNodeClick={onNodeClick}
        nodeTypes={nodeTypes}
        fitView
        minZoom={0.1}
        maxZoom={2}
        defaultViewport={{ x: 0, y: 0, zoom: 1 }}
        attributionPosition="bottom-left"
      >
        <Controls 
          position="top-left"
          className="bg-white shadow-md rounded-md"
        />
        
        <MiniMap 
          position="top-right"
          style={miniMapStyle}
          className="bg-white rounded-md shadow-md"
          maskColor="rgba(0, 0, 0, 0.1)"
        />
        
        <Background 
          variant={BackgroundVariant.Dots} 
          gap={20} 
          size={1} 
          color="#e5e7eb"
        />

        {/* Custom Panels */}
        <Panel position="top-center">
          <div className="bg-white rounded-lg shadow-md p-4 max-w-md">
            <div className="flex items-center justify-between mb-3">
              <h2 className="text-lg font-semibold text-gray-900">
                Knowledge Graph
              </h2>
              <div className="flex items-center space-x-2">
                <button
                  onClick={() => setShowSearch(!showSearch)}
                  className={`p-2 rounded-md transition-colors ${
                    showSearch ? 'bg-blue-100 text-blue-700' : 'bg-gray-100 text-gray-700'
                  }`}
                  title="Toggle Search"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                </button>
                <button
                  onClick={handleFitView}
                  className="p-2 rounded-md bg-gray-100 text-gray-700 hover:bg-gray-200 transition-colors"
                  title="Fit View"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 1v4m0 0h-4m4 0l-5-5" />
                  </svg>
                </button>
                <button
                  onClick={handleRefresh}
                  className="p-2 rounded-md bg-gray-100 text-gray-700 hover:bg-gray-200 transition-colors"
                  title="Refresh"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                  </svg>
                </button>
              </div>
            </div>
            
            {showSearch && (
              <GraphSearch
                query={searchQuery}
                onQueryChange={onSearchQueryChange}
                isSearching={isSearching}
                results={searchResults}
              />
            )}
          </div>
        </Panel>

        {showLegend && (
          <Panel position="bottom-left">
            <GraphLegend onClose={() => setShowLegend(false)} />
          </Panel>
        )}

        {graphData && (
          <Panel position="bottom-right">
            <GraphControls
              nodeCount={graphData.totalNodes}
              edgeCount={graphData.totalEdges}
              onShowLegend={() => setShowLegend(true)}
            />
          </Panel>
        )}
      </ReactFlow>

      {/* Node Details Panel */}
      {selectedNode && (
        <NodeDetailsPanel
          node={selectedNode}
          onClose={() => {
            setSelectedNode(null);
            updateSelectedNode(null);
          }}
        />
      )}
    </div>
  );
};