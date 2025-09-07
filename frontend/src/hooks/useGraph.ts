// Graph hook for Section 3.3 - Graph Visualization
// React hook using TanStack Query for graph data management

import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {useCallback, useState} from 'react';
import GraphService from '../services/graphService';
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import {GraphData, GraphSearchParams, SubgraphParams} from '../types/graph';

// Query keys for React Query
export const GRAPH_QUERY_KEYS = {
  graphData: ['graph', 'data'] as const,
  subgraph: (nodeId: string) => ['graph', 'subgraph', nodeId] as const,
  searchNodes: (query: string) => ['graph', 'search', query] as const,
  stats: ['graph', 'stats'] as const,
  neighbors: (nodeId: string) => ['graph', 'neighbors', nodeId] as const,
};

// Graph data options
interface UseGraphDataOptions {
  maxNodes?: number;
  refetchInterval?: number;
  enabled?: boolean;
}

export const useGraph = () => {
  const queryClient = useQueryClient();
  const graphService = GraphService.getInstance();
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState<string>('');

  /**
   * Main hook for fetching graph data
   */
  const useGraphData = (options?: UseGraphDataOptions) => {
    return useQuery({
      queryKey: [...GRAPH_QUERY_KEYS.graphData, options?.maxNodes],
      queryFn: () => graphService.getGraphData({ maxNodes: options?.maxNodes }),
      staleTime: 5 * 60 * 1000, // 5 minutes
      gcTime: 10 * 60 * 1000, // 10 minutes (renamed from cacheTime)
      refetchInterval: options?.refetchInterval,
      enabled: options?.enabled ?? true,
    });
  };

  /**
   * Hook for fetching subgraph data
   */
  const useSubgraph = (params: SubgraphParams | null) => {
    return useQuery({
      queryKey: params ? GRAPH_QUERY_KEYS.subgraph(params.nodeId) : [],
      queryFn: () => params ? graphService.getSubgraph(params) : Promise.resolve(null),
      enabled: !!params,
      staleTime: 2 * 60 * 1000, // 2 minutes
    });
  };

  /**
   * Hook for searching graph nodes
   */
  const useSearchNodes = (query: string, enabled: boolean = true) => {
    return useQuery({
      queryKey: GRAPH_QUERY_KEYS.searchNodes(query),
      queryFn: () => graphService.searchNodes(query),
      enabled: enabled && query.length > 0,
      staleTime: 1 * 60 * 1000, // 1 minute
    });
  };

  /**
   * Hook for graph statistics
   */
  const useGraphStats = () => {
    return useQuery({
      queryKey: GRAPH_QUERY_KEYS.stats,
      queryFn: () => graphService.getGraphStats(),
      staleTime: 5 * 60 * 1000, // 5 minutes
    });
  };

  /**
   * Hook for getting node neighbors
   */
  const useNodeNeighbors = (nodeId: string | null, maxDepth: number = 1) => {
    return useQuery({
      queryKey: nodeId ? GRAPH_QUERY_KEYS.neighbors(nodeId) : [],
      queryFn: () => nodeId ? graphService.getNeighbors(nodeId, maxDepth) : Promise.resolve(null),
      enabled: !!nodeId,
      staleTime: 2 * 60 * 1000, // 2 minutes
    });
  };

  /**
   * Mutation for advanced search
   */
  const useAdvancedSearch = () => {
    return useMutation({
      mutationFn: (params: GraphSearchParams) => graphService.advancedSearch(params),
    });
  };

  /**
   * Invalidate graph data cache (useful after note/link changes)
   */
  const invalidateGraphData = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: GRAPH_QUERY_KEYS.graphData });
    queryClient.invalidateQueries({ queryKey: GRAPH_QUERY_KEYS.stats });
  }, [queryClient]);

  /**
   * Invalidate specific subgraph cache
   */
  const invalidateSubgraph = useCallback((nodeId: string) => {
    queryClient.invalidateQueries({ queryKey: GRAPH_QUERY_KEYS.subgraph(nodeId) });
  }, [queryClient]);

  /**
   * Refresh all graph-related data
   */
  const refreshGraphData = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ['graph'] });
  }, [queryClient]);

  /**
   * Prefetch subgraph data for faster interactions
   */
  const prefetchSubgraph = useCallback((params: SubgraphParams) => {
    queryClient.prefetchQuery({
      queryKey: GRAPH_QUERY_KEYS.subgraph(params.nodeId),
      queryFn: () => graphService.getSubgraph(params),
      staleTime: 2 * 60 * 1000,
    });
  }, [queryClient]);

  /**
   * Get cached graph data without triggering a fetch
   */
  const getCachedGraphData = useCallback((): GraphData | undefined => {
    return queryClient.getQueryData(GRAPH_QUERY_KEYS.graphData);
  }, [queryClient]);

  /**
   * Update search query state
   */
  const updateSearchQuery = useCallback((query: string) => {
    setSearchQuery(query);
  }, []);

  /**
   * Update selected node state
   */
  const updateSelectedNode = useCallback((nodeId: string | null) => {
    setSelectedNodeId(nodeId);
  }, []);

  // Return all hooks and utilities
  return {
    // Hooks
    useGraphData,
    useSubgraph,
    useSearchNodes,
    useGraphStats,
    useNodeNeighbors,
    useAdvancedSearch,
    
    // Cache management
    invalidateGraphData,
    invalidateSubgraph,
    refreshGraphData,
    prefetchSubgraph,
    getCachedGraphData,
    
    // State management
    selectedNodeId,
    searchQuery,
    updateSearchQuery,
    updateSelectedNode,
    
    // Direct service access for custom operations
    graphService,
  };
};

export default useGraph;