// Graph Visualization Types for Section 3.3
// TypeScript interfaces for graph data, nodes, edges, and visualization

export interface GraphNode {
  id: string;
  title: string;
  content: string;
  tags: string[];
  size: number; // Visual size based on content length and connections
  color: string; // Color based on tags or recency
  connectionCount: number;
  highlighted: boolean;
  // React Flow positioning
  x?: number;
  y?: number;
  // Additional metadata
  createdAt?: string;
  updatedAt?: string;
}

export interface GraphEdge {
  id: string;
  source: string; // Source node ID
  target: string; // Target node ID
  type?: string; // Relationship type
  context?: string; // Link context
  strength?: number; // Relationship strength (0-1)
  color?: string; // Edge color
  width?: number; // Edge width
  highlighted?: boolean;
  bidirectional?: boolean;
}

export interface GraphData {
  nodes: GraphNode[];
  edges: GraphEdge[];
  totalNodes: number;
  totalEdges: number;
}

export interface GraphStats {
  totalNodes: number;
  totalEdges: number;
  averageConnections: number;
  mostConnectedNode?: GraphNode;
  clusters?: GraphCluster[];
}

export interface GraphCluster {
  id: string;
  nodes: string[]; // Node IDs in this cluster
  centroid: { x: number; y: number };
  color: string;
}

// React Flow specific types
export interface ReactFlowNode {
  id: string;
  position: { x: number; y: number };
  data: {
    label: string;
    note: GraphNode;
    isSelected?: boolean;
    isHighlighted?: boolean;
  };
  type?: string;
  style?: Record<string, any>;
}

export interface ReactFlowEdge {
  id: string;
  source: string;
  target: string;
  type?: string;
  style?: Record<string, any>;
  animated?: boolean;
  data?: {
    relationship: GraphEdge;
    isHighlighted?: boolean;
  };
}

// Search and filtering
export interface GraphSearchParams {
  query: string;
  nodeTypes?: string[];
  relationshipTypes?: string[];
  tags?: string[];
}

export interface GraphSearchResult {
  nodes: GraphNode[];
  highlightedTerms: string[];
  totalMatches: number;
}

// Visualization configuration
export interface GraphVisualizationConfig {
  maxNodes?: number;
  layout?: 'force' | 'hierarchical' | 'circular' | 'grid';
  clustering?: boolean;
  showLabels?: boolean;
  showEdgeLabels?: boolean;
  nodeSize?: 'fixed' | 'byConnections' | 'byContent';
  nodeColor?: 'byTags' | 'byRecency' | 'byType' | 'uniform';
  edgeThickness?: 'uniform' | 'byStrength';
  animations?: boolean;
}

// Subgraph types
export interface SubgraphParams {
  nodeId: string;
  depth?: number;
  maxNodes?: number;
  includeIncoming?: boolean;
  includeOutgoing?: boolean;
}

// Graph interaction types
export interface NodeInteraction {
  type: 'click' | 'hover' | 'doubleClick' | 'rightClick';
  node: GraphNode;
  position: { x: number; y: number };
  event: MouseEvent;
}

export interface EdgeInteraction {
  type: 'click' | 'hover';
  edge: GraphEdge;
  position: { x: number; y: number };
  event: MouseEvent;
}

// Graph layout types
export interface GraphLayout {
  name: string;
  positions: Array<{ id: string; x: number; y: number }>;
  bounds: { minX: number; maxX: number; minY: number; maxY: number };
}

export interface ForceLayoutOptions {
  linkDistance?: number;
  linkStrength?: number;
  chargeStrength?: number;
  centeringStrength?: number;
  collisionRadius?: number;
  velocityDecay?: number;
}

// Export all for convenience
export type {
  GraphNode as Node,
  GraphEdge as Edge,
  GraphData as Data
};