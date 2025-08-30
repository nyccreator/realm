// Graph service for Section 3.3 - Graph Visualization
// Handles all API calls to the backend graph visualization endpoints

import {GraphData, GraphNode, GraphSearchParams, GraphSearchResult, GraphStats, SubgraphParams} from '../types/graph';
import {ApiError} from '../types/note';
import AuthService from './authService';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
const GRAPH_BASE_URL = `${API_BASE_URL}/graph`;

class GraphService {
  private static instance: GraphService;
  private authService = AuthService.getInstance();

  public static getInstance(): GraphService {
    if (!GraphService.instance) {
      GraphService.instance = new GraphService();
    }
    return GraphService.instance;
  }

  /**
   * Get authorization headers for API requests
   */
  private getAuthHeaders(): Record<string, string> {
    const token = this.authService.getStoredAccessToken();
    return {
      'Content-Type': 'application/json',
      ...(token && { 'Authorization': `Bearer ${token}` })
    };
  }

  /**
   * Handle API response errors
   */
  private async handleResponse<T>(response: Response): Promise<T> {
    const data = await response.json();
    
    if (!response.ok) {
      const error: ApiError = {
        message: data.message || 'Request failed',
        status: response.status,
        details: data.details || []
      };
      throw error;
    }

    return data as T;
  }

  /**
   * Get complete graph data for visualization
   */
  async getGraphData(options?: { maxNodes?: number }): Promise<GraphData> {
    const queryParams = new URLSearchParams();
    
    if (options?.maxNodes) {
      queryParams.append('maxNodes', options.maxNodes.toString());
    }

    const response = await fetch(`${GRAPH_BASE_URL}/data?${queryParams}`, {
      method: 'GET',
      headers: this.getAuthHeaders(),
    });

    return this.handleResponse<GraphData>(response);
  }

  /**
   * Get subgraph focused on a specific node
   */
  async getSubgraph(params: SubgraphParams): Promise<GraphData> {
    const { nodeId, depth, maxNodes, includeIncoming, includeOutgoing } = params;
    const queryParams = new URLSearchParams();
    
    if (depth !== undefined) {
      queryParams.append('depth', depth.toString());
    }
    if (maxNodes !== undefined) {
      queryParams.append('maxNodes', maxNodes.toString());
    }
    if (includeIncoming !== undefined) {
      queryParams.append('includeIncoming', includeIncoming.toString());
    }
    if (includeOutgoing !== undefined) {
      queryParams.append('includeOutgoing', includeOutgoing.toString());
    }

    const response = await fetch(`${GRAPH_BASE_URL}/subgraph/${nodeId}?${queryParams}`, {
      method: 'GET',
      headers: this.getAuthHeaders(),
    });

    return this.handleResponse<GraphData>(response);
  }

  /**
   * Search graph nodes
   */
  async searchNodes(query: string): Promise<GraphNode[]> {
    const queryParams = new URLSearchParams();
    queryParams.append('query', query);

    const response = await fetch(`${GRAPH_BASE_URL}/search?${queryParams}`, {
      method: 'GET',
      headers: this.getAuthHeaders(),
    });

    return this.handleResponse<GraphNode[]>(response);
  }

  /**
   * Get graph statistics
   */
  async getGraphStats(): Promise<GraphStats> {
    const response = await fetch(`${GRAPH_BASE_URL}/stats`, {
      method: 'GET',
      headers: this.getAuthHeaders(),
    });

    return this.handleResponse<GraphStats>(response);
  }

  /**
   * Advanced search with multiple parameters
   */
  async advancedSearch(params: GraphSearchParams): Promise<GraphSearchResult> {
    const queryParams = new URLSearchParams();
    queryParams.append('query', params.query);
    
    if (params.nodeTypes && params.nodeTypes.length > 0) {
      params.nodeTypes.forEach(type => queryParams.append('nodeType', type));
    }
    
    if (params.relationshipTypes && params.relationshipTypes.length > 0) {
      params.relationshipTypes.forEach(type => queryParams.append('relationshipType', type));
    }
    
    if (params.tags && params.tags.length > 0) {
      params.tags.forEach(tag => queryParams.append('tag', tag));
    }

    const response = await fetch(`${GRAPH_BASE_URL}/search/advanced?${queryParams}`, {
      method: 'GET',
      headers: this.getAuthHeaders(),
    });

    return this.handleResponse<GraphSearchResult>(response);
  }

  /**
   * Get neighboring nodes for a specific node
   */
  async getNeighbors(nodeId: string, maxDepth: number = 1): Promise<GraphData> {
    const queryParams = new URLSearchParams();
    queryParams.append('maxDepth', maxDepth.toString());

    const response = await fetch(`${GRAPH_BASE_URL}/neighbors/${nodeId}?${queryParams}`, {
      method: 'GET',
      headers: this.getAuthHeaders(),
    });

    return this.handleResponse<GraphData>(response);
  }

  /**
   * Get shortest path between two nodes
   */
  async getShortestPath(sourceNodeId: string, targetNodeId: string): Promise<{
    path: GraphNode[];
    relationships: string[];
    distance: number;
  }> {
    const response = await fetch(`${GRAPH_BASE_URL}/path/${sourceNodeId}/${targetNodeId}`, {
      method: 'GET',
      headers: this.getAuthHeaders(),
    });

    return this.handleResponse<{
      path: GraphNode[];
      relationships: string[];
      distance: number;
    }>(response);
  }

  /**
   * Get centrality metrics for graph analysis
   */
  async getCentralityMetrics(): Promise<{
    mostCentral: GraphNode[];
    mostConnected: GraphNode[];
    bridges: GraphNode[];
  }> {
    const response = await fetch(`${GRAPH_BASE_URL}/metrics/centrality`, {
      method: 'GET',
      headers: this.getAuthHeaders(),
    });

    return this.handleResponse<{
      mostCentral: GraphNode[];
      mostConnected: GraphNode[];
      bridges: GraphNode[];
    }>(response);
  }
}

export default GraphService;